package com.github.phoswald.rstm.databind;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class Databinder {

    private final Map<Class<?>, ClassInfo<?>> classes = new ConcurrentHashMap<>();

    public <T> Object access(Class<T> clazz, T instance, String name) {
        ClassInfo<T> classInfo = getClassInfo(clazz);
        return classInfo.fields().get(name).getter().apply(instance);
    }

    public <T> Map<String, Object> extract(Class<T> clazz, T instance) {
        ClassInfo<T> classInfo = getClassInfo(clazz);
        Map<String, Object> map = new LinkedHashMap<>();
        for (FieldInfo<T> fieldInfo : classInfo.fields().values()) {
            map.put(fieldInfo.name(), fieldInfo.getter().apply(instance));
        }
        return map;
    }

    public <T> T create(Class<T> clazz, Map<String, Object> map) {
        ClassInfo<T> classInfo = getClassInfo(clazz);
        Object[] args = new Object[classInfo.fields().size()];
        for (FieldInfo<T> fieldInfo : classInfo.fields().values()) {
            args[fieldInfo.index()] = coerce(fieldInfo, map.get(fieldInfo.name()));
        }
        for (String name : map.keySet()) {
            if(!classInfo.fields().containsKey(name)) {
                throw new DatabinderException("Invalid field for " + clazz + ": " + name);
            }
        }
        return classInfo.constructor().apply(args);
    }

    public <T> byte[] toXml(Class<T> clazz, T instance) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try(DataOutputStream stream = new XmlOutputStream(buffer)) {
            toStream(clazz, instance, stream);
        } catch (Exception e) {
            throw new DatabinderException(e);
        }
        return buffer.toByteArray();
    }

    public String toJson(Object instance) { // TODO (API): should be compatible with JSON-B
        return new String(toJson((Class) instance.getClass(), instance), UTF_8);
    }

    public <T> byte[] toJson(Class<T> clazz, T instance) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try(DataOutputStream stream = new JsonOutputStream(buffer)) {
            toStream(clazz, instance, stream);
        } catch (Exception e) {
            throw new DatabinderException(e);
        }
        return buffer.toByteArray();
    }

    private <T> void toStream(Class<T> clazz, T instance, DataOutputStream stream) throws Exception {
        ClassInfo<T> classInfo = getClassInfo(clazz);
        stream.startObject(classInfo.name());
        for (FieldInfo<T> fieldInfo : classInfo.fields().values()) {
            stream.field(fieldInfo.name(), fieldInfo.getter().apply(instance));
        }
        stream.endObject(classInfo.name());
    }

    public <T> T fromXml(Class<T> clazz, byte[] xml) {
        InputStream buffer = new ByteArrayInputStream(xml);
        try(XmlInputStream stream = new XmlInputStream(buffer)) {
            return fromStream(clazz, stream);
        } catch (Exception e) {
            throw new DatabinderException(e);
        }
    }

    public <T> T fromJson(Class<T> clazz, String json) { // TODO (API): should be compatible with JSON-B
        return fromJson(clazz, json.getBytes(UTF_8));
    }

    public <T> T fromJson(Class<T> clazz, byte[] json) {
        InputStream buffer = new ByteArrayInputStream(json);
        try(DataInputStream stream = new JsonInputStream(buffer)) {
            return fromStream(clazz, stream);
        } catch (Exception e) {
            throw new DatabinderException(e);
        }
    }

    private <T> T fromStream(Class<T> clazz, DataInputStream stream) throws Exception {
        ClassInfo<T> classInfo = getClassInfo(clazz);
        Map<String, Object> map = new LinkedHashMap<>();
        stream.readObject(classInfo, map);
        return create(clazz, map);
    }

    @SuppressWarnings("unchecked")
    private <T> ClassInfo<T> getClassInfo(Class<T> clazz) {
        return (ClassInfo<T>) classes.computeIfAbsent(clazz, Databinder::createClassInfo);
    }

    private static <T> ClassInfo<T> createClassInfo(Class<T> clazz) {
        if(!clazz.isRecord()) {
            throw new DatabinderException("Not a record: " + clazz);
        }
        RecordComponent[] components = clazz.getRecordComponents();
        Map<String, FieldInfo<T>> fields = new LinkedHashMap<>();
        for (int index = 0; index < components.length; index++) {
            RecordComponent component = components[index];
            fields.put(components[index].getName(), new FieldInfo<>( //
                    index, //
                    component.getName(), //
                    component.getType(), //
                    instance -> get(instance, component.getAccessor())));
        }
        Constructor<T> constructor = constructor(clazz);
        return new ClassInfo<T>( //
                getName(clazz), //
                clazz, //
                args -> construct(constructor, args), //
                fields);
    }

    private static <T> Object get(T instance, Method method) {
        try {
            method.setAccessible(true);
            return method.invoke(instance);
        } catch (ReflectiveOperationException e) {
            throw new DatabinderException(e);
        }
    }

    private static Object coerce(FieldInfo<?> fieldInfo, Object value) {
        if(value == null) {
            if(fieldInfo.clazz() == byte.class) {
                return (byte) 0;
            }
            if(fieldInfo.clazz() == short.class) {
                return (short) 0;
            }
            if(fieldInfo.clazz() == int.class) {
                return 0;
            }
            if(fieldInfo.clazz() == long.class) {
                return 0L;
            }
            if(fieldInfo.clazz() == float.class) {
                return 0.0f;
            }
            if(fieldInfo.clazz() == double.class) {
                return 0.0;
            }
            if(fieldInfo.clazz() == boolean.class) {
                return false;
            }
            if(fieldInfo.clazz() == char.class) {
                return '0'; // XXX: represent '\0'
            }
            return null;
        }
        if(value.getClass() == fieldInfo.clazz()) {
            return value;
        }
        if(fieldInfo.clazz() == String.class) {
            return value.toString();
        }
        if(fieldInfo.clazz() == Byte.class || fieldInfo.clazz() == byte.class) {
            return Byte.valueOf(value.toString());
        }
        if(fieldInfo.clazz() == Short.class || fieldInfo.clazz() == short.class) {
            return Short.valueOf(value.toString());
        }
        if(fieldInfo.clazz() == Integer.class || fieldInfo.clazz() == int.class) {
            return Integer.valueOf(value.toString());
        }
        if(fieldInfo.clazz() == Long.class || fieldInfo.clazz() == long.class) {
            return Long.valueOf(value.toString());
        }
        if(fieldInfo.clazz() == Float.class || fieldInfo.clazz() == float.class) {
            return Float.valueOf(value.toString());
        }
        if(fieldInfo.clazz() == Double.class || fieldInfo.clazz() == double.class) {
            return Double.valueOf(value.toString());
        }
        if(fieldInfo.clazz() == Boolean.class || fieldInfo.clazz() == boolean.class) {
            return Boolean.valueOf(value.toString()); // XXX: parse boolean strictly
        }
        if(fieldInfo.clazz() == Character.class || fieldInfo.clazz() == char.class) {
            return Character.valueOf(value.toString().charAt(0)); // XXX: parse character strictly
        }
        throw new DatabinderException("Cannot coerce to " + fieldInfo.clazz() + " from " + value.getClass());
    }

    private static <T> Constructor<T> constructor(Class<T> clazz) {
        try {
            Class<?>[] args = Arrays.stream(clazz.getRecordComponents())//
                    .map(RecordComponent::getType) //
                    .toArray(Class<?>[]::new);
            return clazz.getDeclaredConstructor(args);
        } catch (ReflectiveOperationException e) {
            throw new DatabinderException(e);
        }
    }

    private static <T> T construct(Constructor<T> constructor, Object[] args) {
        try {
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new DatabinderException(e);
        }
    }

    private static String getName(Class<?> clazz) {
        String name = clazz.getSimpleName();
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }

    static record ClassInfo<T>( //
            String name, //
            Class<T> clazz, //
            Function<Object[], T> constructor, //
            Map<String, FieldInfo<T>> fields //
    ) {

    }

    static record FieldInfo<T>( //
            int index, //
            String name, //
            Class<?> clazz, //
            Function<T, Object> getter //
    ) {

    }
}
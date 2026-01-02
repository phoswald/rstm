package com.github.phoswald.rstm.databind;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

record ClassInfo(
        String name,
        Class<?> clazz,
        Function<Object[], Object> constructor,
        Deferred<Map<String, FieldInfo>> fields
) {
    static ClassInfo create(Class<?> clazz) {
        Constructor<?> constructor = getConstructor(clazz);
        return new ClassInfo(
                getName(clazz),
                clazz,
                args -> construct(constructor, args),
                new Deferred<>());
    }

    Object accessField(Object instance, String name) {
        return fields.access().get(name).getter().apply(instance);
    }

    Map<String, Object> extractFields(Object instance) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (FieldInfo fieldInfo : fields.access().values()) {
            map.put(fieldInfo.name(), fieldInfo.getter().apply(instance));
        }
        return map;
    }

    Object createInstance(Map<String, Object> map) {
        Map<String, FieldInfo> fields = this.fields.access();
        Object[] args = new Object[fields.size()];
        int index = 0;
        for (FieldInfo fieldInfo : fields.values()) {
            args[index++] = fieldInfo.type().coerce(map.get(fieldInfo.name()));
        }
        for (String name : map.keySet()) {
            if(!fields.containsKey(name)) {
                throw new DatabinderException("Invalid field for " + clazz + ": " + name);
            }
        }
        return constructor.apply(args);
    }

    private static String getName(Class<?> clazz) {
        String name = clazz.getSimpleName();
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }

    private static Constructor<?> getConstructor(Class<?> clazz) {
        try {
            Class<?>[] args = Arrays.stream(clazz.getRecordComponents())
                    .map(RecordComponent::getType)
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
}

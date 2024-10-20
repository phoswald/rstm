package com.github.phoswald.rstm.databind;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Databinder {

    private final boolean pretty;
    private final boolean tolerant;
    private final Map<Class<?>, ClassInfo> classes = new HashMap<>();

    public Databinder() {
        this(true, true);
    }

    public Databinder pretty(boolean pretty) {
        return new Databinder(pretty, tolerant);
    }

    public Databinder tolerant(boolean tolerant) {
        return new Databinder(pretty, tolerant);
    }

    private Databinder(boolean pretty, boolean tolerant) {
        this.pretty = pretty;
        this.tolerant = tolerant;
    }

    public <T> Object accessField(Class<T> clazz, T instance, String name) {
        return getClassInfo(clazz).accessField(instance, name);
    }

    public <T> Map<String, Object> extractFields(Class<T> clazz, T instance) {
        return getClassInfo(clazz).extractFields(instance);
    }

    public <T> T createInstance(Class<T> clazz, Map<String, Object> map) {
        return clazz.cast(getClassInfo(clazz).createInstance(map));
    }

    public String toXml(Object instance) {
        StringWriter buffer = new StringWriter();
        toXml(instance, buffer);
        return buffer.toString();
    }

    public void toXml(Object instance, OutputStream stream) {
        toXml(instance, new OutputStreamWriter(stream, UTF_8));
    }

    public void toXml(Object instance, Writer writer) {
        try(DataOutputStream stream = new XmlOutputStream(writer, pretty)) {
            stream.writeObject(getClassInfo(instance.getClass()), instance);
        } catch (DatabinderException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabinderException(e);
        }
    }

    public String toJson(Object instance) {
        StringWriter buffer = new StringWriter();
        toJson(instance, buffer);
        return buffer.toString();
    }

    public void toJson(Object instance, OutputStream stream) {
        toJson(instance, new OutputStreamWriter(stream, UTF_8));
    }

    public void toJson(Object instance, Writer writer) {
        try(DataOutputStream stream = new JsonOutputStream(writer, pretty)) {
            stream.writeObject(getClassInfo(instance.getClass()), instance);
        } catch (DatabinderException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabinderException(e);
        }
    }

    public <T> T fromXml(String string, Class<T> clazz) {
        return fromXml(new StringReader(string), clazz);
    }

    public <T> T fromXml(InputStream stream, Class<T> clazz) {
        return fromXml(new InputStreamReader(stream, UTF_8), clazz); // TODO (correctness): reader should not assume UTF-8 stream
    }

    public <T> T fromXml(Reader reader, Class<T> clazz) {
        try(DataInputStream stream = new XmlInputStream(reader, tolerant)) {
            return clazz.cast(stream.readObject(getClassInfo(clazz)));
        } catch (DatabinderException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabinderException(e);
        }
    }

    public <T> T fromJson(String string, Class<T> clazz) {
        return fromJson(new StringReader(string), clazz);
    }

    public <T> T fromJson(InputStream stream, Class<T> clazz) {
        return fromJson(new InputStreamReader(stream, UTF_8), clazz);
    }

    public <T> T fromJson(Reader reader, Class<T> clazz) {
        try(DataInputStream stream = new JsonInputStream(reader, tolerant)) {
            return clazz.cast(stream.readObject(getClassInfo(clazz)));
        } catch (DatabinderException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabinderException(e);
        }
    }

    private ClassInfo getClassInfo(Class<?> clazz) {
        synchronized (classes) {
            ClassInfo classInfo = classes.get(clazz);
            if(classInfo == null) {
                if(!clazz.isRecord()) {
                    throw new DatabinderException("Not a record: " + clazz);
                }
                classInfo = ClassInfo.create(clazz);
                classes.put(clazz, classInfo);
                createClassInfoFields(classInfo, clazz);
            }
            return classInfo;
        }
    }

    private void createClassInfoFields(ClassInfo classInfo, Class<?> clazz) {
        RecordComponent[] components = clazz.getRecordComponents();
        Map<String, FieldInfo> fields = new LinkedHashMap<>();
        for (RecordComponent component : components) {
            FieldInfo fieldInfo = FieldInfo.create(component, createType(component.getGenericType()));
            fields.put(fieldInfo.name(), fieldInfo);
        }
        classInfo.fields().define(fields);
    }

    private AnyType createType(Type genericType) {
        if(genericType instanceof Class clazz) {
            AnyType type;
            if((type = PrimitiveType.instances.get(clazz)) != null) {
                return type;
            }
            if((type = SimpleType.instances.get(clazz)) != null) {
                return type;
            }
            if(clazz.isRecord()) {
                return new RecordType(getClassInfo(clazz));
            }
        } else if(genericType instanceof ParameterizedType paramType) {
            if(paramType.getRawType() == List.class && paramType.getActualTypeArguments().length == 1) {
                return new ListType((ElementType) createType(paramType.getActualTypeArguments()[0]));
            }
        }
        throw new DatabinderException("Unsupported type: " + genericType);
    }
}

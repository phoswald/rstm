package com.github.phoswald.rstm.databind;

import java.util.List;
import java.util.Map;

abstract class DataOutputStream implements AutoCloseable {

    void writeObject(ClassInfo classInfo, Object instance) throws Exception {
        writeObject(classInfo, classInfo.name(), instance);
    }

    private void writeObject(ClassInfo classInfo, String name, Object instance) throws Exception {
        writeStartObject(name);
        for (FieldInfo fieldInfo : classInfo.fields().access().values()) {
            Object fieldValue = fieldInfo.getter().apply(instance);
            if (fieldValue != null) {
                switch (fieldInfo.type()) {
                    case PrimitiveType primitiveType -> writeValue(fieldInfo.name(), fieldValue);
                    case SimpleType simpleType -> writeValue(fieldInfo.name(), simpleType.format(fieldValue));
                    case RecordType recordType -> writeObject(recordType.classInfo(), fieldInfo.name(), fieldValue);
                    case ListType listType -> writeList(fieldInfo, listType, (List<?>) fieldValue);
                    case MapType mapType -> writeMap(fieldInfo, mapType, (Map<?, ?>) fieldValue);
                }
            }
        }
        writeEndObject(classInfo.name());
    }

    private void writeList(FieldInfo fieldInfo, ListType listType, List<?> listInstance) throws Exception {
        writeStartList(fieldInfo.name());
        for (Object listElement : listInstance) {
            switch (listType.elementType()) {
                case SimpleType simpleType -> writeValue(fieldInfo.name(), simpleType.format(listElement));
                case RecordType recordType -> writeObject(recordType.classInfo(), fieldInfo.name(), listElement);
            }
        }
        writeEndList(fieldInfo.name());
    }

    private void writeMap(FieldInfo fieldInfo, MapType mapType, Map<?, ?> mapInstance) throws Exception {
        writeStartMap(fieldInfo.name());
        for (Map.Entry<?, ?> mapEntry : mapInstance.entrySet()) {
            String mapKey = mapEntry.getKey().toString();
            Object mapElement = mapEntry.getValue();
            switch(mapType.elementType()) {
                case SimpleType simpleType -> writeValue(mapKey, simpleType.format(mapElement));
                case RecordType recordType -> writeObject(recordType.classInfo(), mapKey, mapElement);
            }
        }
        writeEndMap(fieldInfo.name());
    }

    abstract void writeStartObject(String name) throws Exception;

    abstract void writeEndObject(String name) throws Exception;

    abstract void writeStartList(String name) throws Exception;

    abstract void writeEndList(String name) throws Exception;

    abstract void writeStartMap(String name) throws Exception;

    abstract void writeEndMap(String name) throws Exception;

    abstract void writeValue(String name, Object value) throws Exception;
}

package com.github.phoswald.rstm.databind;

import java.util.List;

abstract class DataOutputStream implements AutoCloseable {

    void writeObject(ClassInfo classInfo, Object instance) throws Exception {
        writeObject(classInfo, classInfo.name(), instance);
    }

    private void writeObject(ClassInfo classInfo, String name, Object instance) throws Exception {
        writeStartObject(name);
        for (FieldInfo fieldInfo : classInfo.fields().access().values()) {
            Object fieldValue = fieldInfo.getter().apply(instance);
            if(fieldValue != null) {
                switch (fieldInfo.type()) {
                    case PrimitiveType primitiveType -> writeValue(fieldInfo.name(), fieldValue);
                    case SimpleType simpleType -> writeValue(fieldInfo.name(), simpleType.format(fieldValue));
                    case RecordType recordType -> writeObject(recordType.classInfo(), fieldInfo.name(), fieldValue);
                    case ListType listType -> writeList(fieldInfo, listType, (List<?>) fieldValue);
                }
            }
        }
        writeEndObject(classInfo.name());
    }

    private void writeList(FieldInfo fieldInfo, ListType listType, List<?> listInstance) throws Exception {
        writeStartList(fieldInfo.name());
        for(Object listElement : listInstance) {
            switch(listType.elementType()) {
                case SimpleType simpleType -> writeValue(fieldInfo.name(), simpleType.format(listElement));
                case RecordType recordType -> writeObject(recordType.classInfo(), fieldInfo.name(), listElement);
            }
        }
        writeEndList(fieldInfo.name());
    }

    abstract void writeStartObject(String name) throws Exception;

    abstract void writeEndObject(String name) throws Exception;

    abstract void writeStartList(String name) throws Exception;

    abstract void writeEndList(String name) throws Exception;

    abstract void writeValue(String name, Object value) throws Exception;
}

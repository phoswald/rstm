package com.github.phoswald.rstm.databind;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

abstract class DataInputStream implements AutoCloseable {

    protected final Stack<Scope> stack = new Stack<>();

    abstract Object readObject(ClassInfo classInfo) throws Exception;

    protected static class Scope {

        final ClassInfo classInfo;
        final Map<String, Object> map = new LinkedHashMap<>();

        int level;
        FieldInfo prevFieldInfo = null;
        FieldInfo fieldInfo = null;
        List<Object> listValue = null;

        Scope(ClassInfo classInfo) {
            this.classInfo = classInfo;
        }

        boolean hasFieldChanged() {
            return fieldInfo != prevFieldInfo;
        }

        boolean startField(String name, boolean tolerant) {
            prevFieldInfo = fieldInfo;
            fieldInfo = classInfo.fields().access().get(name);
            if (fieldInfo == null && !tolerant) {
                throw new DatabinderException("Unknown field for " + classInfo.clazz() + ": " + name);
            }
            return fieldInfo != null;
        }

        void startList() {
            if (fieldInfo != null) {
                listValue = new ArrayList<>();
                map.put(fieldInfo.name(), listValue);
            }
        }

        void endList() {
            listValue = null;
        }

        void addValue(Object value) {
            if (listValue != null) {
                listValue.add(value);
            } else if (fieldInfo != null) {
                map.put(fieldInfo.name(), value);
            }
        }

        Object createInstance() {
            return classInfo.createInstance(map);
        }
    }
}

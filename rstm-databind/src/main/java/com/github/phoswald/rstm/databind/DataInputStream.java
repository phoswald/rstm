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
        String mapKey = null;
        Map<String, Object> mapValue = null;

        Scope(ClassInfo classInfo) {
            System.out.println("scope " + (classInfo == null ? "MAP" : classInfo.name())); // XXX
            this.classInfo = classInfo;
        }

        boolean hasFieldChanged() {
            return fieldInfo != prevFieldInfo;
        }

        boolean startField(String name, boolean tolerant) {
            if(mapValue != null) {
                System.out.println("start [" + name + "]");
                mapKey = name;
                return true;
            }
            System.out.println("start " + classInfo.name() + "." + name);
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

        boolean isMap() {
            return mapValue != null;
        }

        void startMap() {
            System.out.println("start map");
            mapValue = new LinkedHashMap<>();
            if(fieldInfo != null) {
                map.put(fieldInfo.name(), mapValue);
            }
        }

        void endMap() {
            System.out.println("end map: " + mapValue);
            mapValue = null;
        }

        void addValue(Object value) {
            if (listValue != null) {
                listValue.add(value);
            } else if (mapValue != null) {
                mapValue.put(mapKey, value);
            } else if (fieldInfo != null) {
                map.put(fieldInfo.name(), value);
            }
        }

        Object createInstance() {
            return classInfo.createInstance(map);
        }
    }
}

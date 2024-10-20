package com.github.phoswald.rstm.databind;

record RecordType(ClassInfo classInfo) implements AnyType, ElementType {

    @Override
    public Object coerce(Object value) {
        return classInfo.clazz().cast(value);
    }
}

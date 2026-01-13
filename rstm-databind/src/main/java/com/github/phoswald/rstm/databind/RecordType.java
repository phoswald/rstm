package com.github.phoswald.rstm.databind;

record RecordType(ClassInfo classInfo) implements AnyType, ElementType {

    @Override
    public Kind kind() {
        return Kind.RECORD;
    }

    @Override
    public Class<?> clazz() {
        return classInfo().clazz();
    }

    @Override
    public Object coerce(Object value) {
        return classInfo.clazz().cast(value);
    }
}

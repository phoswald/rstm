package com.github.phoswald.rstm.databind;

sealed interface AnyType permits PrimitiveType, SimpleType, RecordType, ListType, MapType, ElementType {

    Kind kind();

    Class<?> clazz();

    Object coerce(Object value);
}

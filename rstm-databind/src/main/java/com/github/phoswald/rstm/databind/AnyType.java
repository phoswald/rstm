package com.github.phoswald.rstm.databind;

sealed interface AnyType permits PrimitiveType, SimpleType, RecordType, ListType, ElementType {

    Object coerce(Object value);
}

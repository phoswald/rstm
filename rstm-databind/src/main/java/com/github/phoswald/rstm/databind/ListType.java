package com.github.phoswald.rstm.databind;

import java.util.ArrayList;
import java.util.List;

record ListType(ElementType elementType) implements AnyType {

    @Override
    public Object coerce(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof List list) {
            List<Object> result = new ArrayList<>();
            for (Object listElement : list) {
                result.add(elementType.coerce(listElement));
            }
            return result;
        }
        throw new DatabinderException("Cannot coerce to List from " + value.getClass());
    }
}

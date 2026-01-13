package com.github.phoswald.rstm.databind;

import java.util.LinkedHashMap;
import java.util.Map;

record MapType(ElementType elementType) implements AnyType {

    @Override
    public Kind kind() {
        return Kind.MAP;
    }

    @Override
    public Class<?> clazz() {
        return elementType().clazz();
    }

    @Override
    public Object coerce(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> mapEntry : map.entrySet()) {
                result.put(mapEntry.getKey().toString(), elementType.coerce(mapEntry.getValue()));
            }
            return result;
        }
        throw new DatabinderException("Cannot coerce to Map from " + value.getClass());
    }
}

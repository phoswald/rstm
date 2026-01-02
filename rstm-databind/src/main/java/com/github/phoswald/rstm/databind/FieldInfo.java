package com.github.phoswald.rstm.databind;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.function.Function;

record FieldInfo(
        String name,
        AnyType type,
        Function<Object, Object> getter
) {

    static FieldInfo create(RecordComponent component, AnyType type) {
        Method accessor = component.getAccessor();
        return new FieldInfo(
                component.getName(),
                type,
                instance -> get(instance, accessor));
    }

    private static Object get(Object instance, Method accessor) {
        try {
            accessor.setAccessible(true);
            return accessor.invoke(instance);
        } catch (ReflectiveOperationException e) {
            throw new DatabinderException(e);
        }
    }
}

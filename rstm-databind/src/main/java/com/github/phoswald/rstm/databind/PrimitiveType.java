package com.github.phoswald.rstm.databind;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

record PrimitiveType(
        Class<?> clazz,
        Class<?> boxedClass,
        Function<String, Object> parseMethod,
        Object defaultValue
) implements AnyType {

    private static final List<PrimitiveType> list = List.of(
            new PrimitiveType(byte.class, Byte.class, Byte::valueOf, (byte) 0),
            new PrimitiveType(short.class, Short.class, Short::valueOf, (short) 0),
            new PrimitiveType(int.class, Integer.class, Integer::valueOf, 0),
            new PrimitiveType(long.class, Long.class, Long::valueOf, 0L),
            new PrimitiveType(float.class, Float.class, Float::valueOf, 0.0f),
            new PrimitiveType(double.class, Double.class, Double::valueOf, 0.0),
            new PrimitiveType(boolean.class, Boolean.class, PrimitiveType::parseBoolean, false),
            new PrimitiveType(char.class, Character.class, PrimitiveType::parseCharacter, (char) 0));

    static final Map<Class<?>, PrimitiveType> instances = list.stream()
            .collect(toMap(PrimitiveType::clazz, identity()));

    @Override
    public Object coerce(Object value) {
        return value == null ? defaultValue : parseMethod.apply(value.toString());
    }

    private static Boolean parseBoolean(String s) {
        return switch (s) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException("Cannot parse boolean: " + s);
        };
    }

    private static Character parseCharacter(String s) {
        if (s.length() == 1) {
            return s.charAt(0);
        } else {
            throw new IllegalArgumentException("Cannot parse character: " + s);
        }
    }
}

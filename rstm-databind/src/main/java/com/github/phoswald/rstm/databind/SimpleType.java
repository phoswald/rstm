package com.github.phoswald.rstm.databind;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

record SimpleType( //
        Class<?> clazz, //
        Function<String, Object> parseMethod, //
        Function<Object, Object> formatMethod //
) implements AnyType, ElementType {

    private static final DateTimeFormatter INSTANT_PARSE = DateTimeFormatter.ISO_INSTANT;
    private static final DateTimeFormatter INSTANT_FORMAT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .parseStrict()
            .appendInstant(3)
            .toFormatter();

    private static final List<SimpleType> list = combine( //
            PrimitiveType.instances.values().stream().map(SimpleType::fromPrimitiveType).toList(), //
            new SimpleType(String.class, Object::toString, identity()), //
            new SimpleType(Instant.class, SimpleType::parseInstant, SimpleType::formatInstant));

    static final Map<Class<?>, SimpleType> instances = list.stream().collect(toMap(SimpleType::clazz, identity()));

    static SimpleType forEnum(Class<?> clazz) {
        return new SimpleType(clazz, value -> parseEnum(clazz, value), SimpleType::formatEnum);
    }

    @Override
    public Object coerce(Object value) {
        return value == null ? null : clazz.isInstance(value) ? value : parseMethod.apply(value.toString());
    }

    public Object format(Object value) {
        return value == null ? null : formatMethod.apply(value);
    }

    private static List<SimpleType> combine(List<SimpleType> first, SimpleType... more) {
        List<SimpleType> list = new ArrayList<>();
        list.addAll(first);
        list.addAll(List.of(more));
        return list;
    }

    private static SimpleType fromPrimitiveType(PrimitiveType type) {
        return new SimpleType(type.boxedClass(), type.parseMethod(), identity());
    }

    private static Instant parseInstant(String value) {
        return INSTANT_PARSE.parse(value, Instant::from);
    }

    private static String formatInstant(Object value) {
        return INSTANT_FORMAT.format((Instant) value);
    }

    private static Object parseEnum(Class clazz, String name) {
        return Enum.valueOf(clazz, name);
    }

    private static String formatEnum(Object value) {
        return ((Enum) value).name();
    }
}

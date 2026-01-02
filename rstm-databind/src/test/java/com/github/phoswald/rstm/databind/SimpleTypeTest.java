package com.github.phoswald.rstm.databind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SimpleTypeTest {

    @Test
    void roundTripNull() {
        SimpleType testee = SimpleType.instances.get(Instant.class);
        assertNull(testee.coerce(null));
        assertNull(testee.format(null));
    }

    @ParameterizedTest
    @MethodSource("roundTripValues")
    void roundTrip(Object value, String string) {
        Class<?> clazz = value.getClass();
        SimpleType testee = SimpleType.instances.get(clazz);
        Object actualValue = testee.coerce(string);
        assertInstanceOf(clazz, actualValue);
        assertEquals(value, actualValue);
        assertEquals(string, testee.format(value));
    }

    private static List<Arguments> roundTripValues() {
        return List.of(
                Arguments.of("sample", "sample"),
                Arguments.of(Instant.ofEpochMilli(1730754686120L), "2024-11-04T21:11:26.120Z"),
                Arguments.of(Instant.ofEpochMilli(1730754686000L), "2024-11-04T21:11:26.000Z"),
                Arguments.of(Instant.ofEpochMilli(0), "1970-01-01T00:00:00.000Z"));
    }

    @Test
    void roundTripEnum() {
        SimpleType testee = SimpleType.forEnum(SampleEnum.class);
        assertSame(SampleEnum.ONE, testee.coerce("ONE"));
        assertEquals("ONE", testee.format(SampleEnum.ONE));
    }

    @ParameterizedTest
    @MethodSource("coerceValues")
    void coerce(Object value, String string) {
        Class<?> clazz = value.getClass();
        SimpleType testee = SimpleType.instances.get(clazz);
        Object actualValue = testee.coerce(string);
        assertInstanceOf(clazz, actualValue);
        assertEquals(value, actualValue);
    }

    private static List<Arguments> coerceValues() {
        return List.of(
//              Arguments.of(Instant.ofEpochMilli(1730754686000L), "2024-11-04T21:11:26"),
                Arguments.of(Instant.ofEpochMilli(1730754686000L), "2024-11-04T21:11:26Z"),
//              Arguments.of(Instant.ofEpochMilli(1730754686120L), "2024-11-04T21:11:26.12"),
                Arguments.of(Instant.ofEpochMilli(1730754686120L), "2024-11-04T21:11:26.12Z"),
//              Arguments.of(Instant.ofEpochMilli(1730754686120L), "2024-11-04T21:11:26.120"),
                Arguments.of(Instant.ofEpochMilli(1730754686120L), "2024-11-04T21:11:26.120Z"),
                Arguments.of(Instant.ofEpochSecond(1730754686, 123456789), "2024-11-04T21:11:26.123456789Z"));
    }

    @ParameterizedTest
    @MethodSource("coerceInvalidValues")
    void coerceInvalid(Class<?> clazz, String string, Class<? extends Exception> expected) {
        SimpleType testee = SimpleType.instances.get(clazz);
        assertThrows(expected, () -> testee.coerce(string));
    }

    private static List<Arguments> coerceInvalidValues() {
        return List.of(
                Arguments.of(Instant.class, "2024-11-04 21:11:26.120Z", DateTimeParseException.class),
                Arguments.of(Instant.class, "2024-11-04T21:11:26.120", DateTimeParseException.class),
                Arguments.of(Instant.class, "2024-11-31T00:00:00.000Z", DateTimeParseException.class));
    }
}

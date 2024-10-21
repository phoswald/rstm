package com.github.phoswald.rstm.databind;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class DatabinderTest {

    // XXX: test classes
    // XXX: test collections

    private static final Sample INSTANCE = Sample.builder() //
            .stringField("sample") //
            .byteField((byte) 42) //
            .shortField((short) 42) //
            .integerField(42) //
            .longField(42L) //
            .floatField(42.0f) //
            .doubleField(42.0) //
            .booleanField(true) //
            .characterField('A') //
            .bytePField((byte) 42) //
            .shortPField((short) 42) //
            .intPField(42) //
            .longPField(42L) //
            .floatPField(42.0f) //
            .doublePField(42.0) //
            .booleanPField(true) //
            .charPField('A') //
            .build();

    private static final Map<String, Object> MAP = Map.ofEntries( //
            Map.entry("stringField", "sample"), //
            Map.entry("byteField", (byte) 42), //
            Map.entry("shortField", (short) 42), //
            Map.entry("integerField", 42), //
            Map.entry("longField", 42L), //
            Map.entry("floatField", 42.0f), //
            Map.entry("doubleField", 42.0), //
            Map.entry("booleanField", true), //
            Map.entry("characterField", 'A'), //
            Map.entry("bytePField", (byte) 42), //
            Map.entry("shortPField", (short) 42), //
            Map.entry("intPField", 42), //
            Map.entry("longPField", 42L), //
            Map.entry("floatPField", 42.0f), //
            Map.entry("doublePField", 42.0), //
            Map.entry("booleanPField", true), //
            Map.entry("charPField", 'A') /*, //
            Map.entry("nullField", null) */); // XXX: null in map value

    private static final String XML = """
            <?xml version="1.0" ?>
            <sample>
                <stringField>sample</stringField>
                <byteField>42</byteField>
                <shortField>42</shortField>
                <integerField>42</integerField>
                <longField>42</longField>
                <floatField>42.0</floatField>
                <doubleField>42.0</doubleField>
                <booleanField>true</booleanField>
                <characterField>A</characterField>
                <bytePField>42</bytePField>
                <shortPField>42</shortPField>
                <intPField>42</intPField>
                <longPField>42</longPField>
                <floatPField>42.0</floatPField>
                <doublePField>42.0</doublePField>
                <booleanPField>true</booleanPField>
                <charPField>A</charPField>
            </sample>
            """;

    private static final String JSON = """
            {
                "stringField": "sample",
                "byteField": 42,
                "shortField": 42,
                "integerField": 42,
                "longField": 42,
                "floatField": 42.0,
                "doubleField": 42.0,
                "booleanField": true,
                "characterField": "A",
                "bytePField": 42,
                "shortPField": 42,
                "intPField": 42,
                "longPField": 42,
                "floatPField": 42.0,
                "doublePField": 42.0,
                "booleanPField": true,
                "charPField": "A"
            }
            """;

    private final Databinder testee = new Databinder();

    @Test
    void access() {
        assertEquals("sample", testee.access(Sample.class, INSTANCE, "stringField"));
        assertEquals(42, testee.access(Sample.class, INSTANCE, "integerField"));
        assertEquals(42L, testee.access(Sample.class, INSTANCE, "longField"));
        assertNull(testee.access(Sample.class, INSTANCE, "nullField"));
    }

    @Test
    void extract_valid_success() {
        Map<String, Object> map = testee.extract(Sample.class, INSTANCE);

        assertNotNull(map);
        assertEquals(18, map.size());
        assertEquals(List.of("stringField", "byteField", "shortField", "integerField", "longField", "floatField", "doubleField", "booleanField", "characterField", "bytePField", "shortPField", "intPField", "longPField", "floatPField", "doublePField", "booleanPField", "charPField", "nullField"), List.copyOf(map.keySet()));
        assertEquals("sample", map.get("stringField"));
        assertEquals(42, map.get("integerField"));
        assertEquals(42L, map.get("longField"));
        assertNull(map.get("nullField"));
    }

    @Test
    void extract_notRecord_exception() {
        assertThrows(DatabinderException.class, () -> testee.extract(SampleClass.class, new SampleClass()));
    }

    @Test
    void create_valid_success() {
        Sample instance = testee.create(Sample.class, MAP);
        assertEquals(INSTANCE, instance);
    }

    @Test
    void create_validStrings_success() {
        Map<String, Object> map = MAP.entrySet().stream() //
                .map(e -> Map.entry(e.getKey(), e.getValue().toString())) //
                .collect((Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        Sample instance = testee.create(Sample.class, map);
        assertEquals(INSTANCE, instance);
    }

    @Test
    void create_validBytes_success() {
        Map<String, Object> map = Map.ofEntries( //
                Map.entry("stringField", (byte) 42), //
                Map.entry("integerField", (byte) 42), //
                Map.entry("longField", (byte) 42));
        Sample instance = testee.create(Sample.class, map);
        assertNotNull(instance);
        assertEquals("42", instance.stringField());
        assertEquals(42, instance.integerField());
        assertEquals(42L, instance.longField());
    }

    @Test
    void create_validEmpty_success() {
        Sample instance = testee.create(Sample.class, Map.of());
        assertNotNull(instance);
    }

    @Test
    void create_invalidName_exception() {
        assertThrows(DatabinderException.class, () -> testee.create(Sample.class, Map.of("badName", "sample")));
    }

    @Test
    void create_notRecord_exception() {
        assertThrows(DatabinderException.class, () -> testee.create(SampleClass.class, Map.of()));
    }

    @Test
    void toXml() {
        byte[] xml = testee.toXml(Sample.class, INSTANCE);
        assertEquals(XML, new String(xml, UTF_8));
    }

    @Test
    void toJson() {
        byte[] json = testee.toJson(Sample.class, INSTANCE);
        assertEquals(JSON, new String(json, UTF_8));
    }

    @Test
    void fromXml() {
        Sample instance = testee.fromXml(Sample.class, XML.getBytes(UTF_8));
        assertEquals(INSTANCE, instance);
    }

    @Test
    void fromJson() {
        Sample instance = testee.fromJson(Sample.class, JSON.getBytes(UTF_8));
        assertEquals(INSTANCE, instance);
    }
}


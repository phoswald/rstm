package com.github.phoswald.rstm.databind;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class DatabinderTest {

    private static final List<String> FIELDS = List.of( //
            "stringField", //
            "byteField", "shortField", "integerField", "longField", "floatField", "doubleField", "booleanField", "characterField", //
            "bytePField", "shortPField", "intPField", "longPField", "floatPField", "doublePField", "booleanPField", "charPField", //
            "instantField", "recordField", //
            "stringListField", "integerListField", "instantListField", "recordListField");

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
            .instantField(Instant.ofEpochMilli(1730754686000L)) //
            .recordField(new SamplePair("sampleKey", "sampleVal")) //
            .stringListField(List.of("sample1", "sample2")) //
            .integerListField(List.of(42, 43)) //
            .instantListField(List.of(Instant.ofEpochMilli(1730754686000L), Instant.ofEpochMilli(1730754686120L))) //
            .recordListField(List.of(new SamplePair("sampleKey1", "sampleVal1"), new SamplePair("sampleKey2", "sampleVal2")))
            .build();

    private static final Sample INSTANCE_EMPTY = Sample.builder().build();

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
            Map.entry("charPField", 'A'), //
            Map.entry("instantField", Instant.ofEpochMilli(1730754686000L)), //
            Map.entry("recordField", new SamplePair("sampleKey", "sampleVal")), //
            Map.entry("stringListField", List.of("sample1", "sample2")), //
            Map.entry("integerListField", List.of(42, 43)), //
            Map.entry("instantListField", List.of(Instant.ofEpochMilli(1730754686000L), Instant.ofEpochMilli(1730754686120L))), //
            Map.entry("recordListField", List.of(new SamplePair("sampleKey1", "sampleVal1"), new SamplePair("sampleKey2", "sampleVal2"))));

    private static final Map<String, Object> MAP_EMPTY = Map.of();

    private static final String XML = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
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
                <instantField>2024-11-04T21:11:26.000Z</instantField>
                <recordField>
                    <key>sampleKey</key>
                    <val>sampleVal</val>
                </recordField>
                <stringListField>sample1</stringListField>
                <stringListField>sample2</stringListField>
                <integerListField>42</integerListField>
                <integerListField>43</integerListField>
                <instantListField>2024-11-04T21:11:26.000Z</instantListField>
                <instantListField>2024-11-04T21:11:26.120Z</instantListField>
                <recordListField>
                    <key>sampleKey1</key>
                    <val>sampleVal1</val>
                </recordListField>
                <recordListField>
                    <key>sampleKey2</key>
                    <val>sampleVal2</val>
                </recordListField>
            </sample>
            """;

    private static final String XML_EMPTY = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <sample>
                <bytePField>0</bytePField>
                <shortPField>0</shortPField>
                <intPField>0</intPField>
                <longPField>0</longPField>
                <floatPField>0.0</floatPField>
                <doublePField>0.0</doublePField>
                <booleanPField>false</booleanPField>
                <charPField>\u0000</charPField>
            </sample>
            """; // TODO (correctness): x0000 is not allowed by XML (but generated by XMLStreamWriter)

    private static final String XML_UNKNOWN = """
            <sample>
                <unknown>foo</unknown>
                <unknownList>foo</unknownList>
                <unknownList>bar</unknownList>
                <unknownMatrix>
                  <unknownMatrix>1</unknownMatrix>
                  <unknownMatrix>2</unknownMatrix>
                </unknownMatrix>
                <unknownMatrix>
                  <unknownMatrix>3</unknownMatrix>
                  <unknownMatrix>4</unknownMatrix>
                </unknownMatrix>
                <recordField>
                    <key>sampleKey</key>
                    <val>sampleVal</val>
                    <unknown>foo</unknown>
                    <unknownList>foo</unknownList>
                    <unknownList>bar</unknownList>
                    <unknownMatrix>
                      <unknownMatrix>1</unknownMatrix>
                      <unknownMatrix>2</unknownMatrix>
                    </unknownMatrix>
                    <unknownMatrix>
                      <unknownMatrix>3</unknownMatrix>
                      <unknownMatrix>4</unknownMatrix>
                    </unknownMatrix>
                </recordField>
                <stringField>sample</stringField>
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
                "charPField": "A",
                "instantField": "2024-11-04T21:11:26.000Z",
                "recordField": {
                    "key": "sampleKey",
                    "val": "sampleVal"
                },
                "stringListField": [
                    "sample1",
                    "sample2"
                ],
                "integerListField": [
                    42,
                    43
                ],
                "instantListField": [
                    "2024-11-04T21:11:26.000Z",
                    "2024-11-04T21:11:26.120Z"
                ],
                "recordListField": [
                    {
                        "key": "sampleKey1",
                        "val": "sampleVal1"
                    },
                    {
                        "key": "sampleKey2",
                        "val": "sampleVal2"
                    }
                ]
            }
            """;

    private static final String JSON_EMPTY = """
            {
                "bytePField": 0,
                "shortPField": 0,
                "intPField": 0,
                "longPField": 0,
                "floatPField": 0.0,
                "doublePField": 0.0,
                "booleanPField": false,
                "charPField": "\\u0000"
            }
            """;

    private static final String JSON_UNKNOWN = """
            {
                "unknown": "foo",
                "unknownList": [ "foo", "bar" ],
                "unknownMatrix": [ [ 1, 2 ], [ 3, 4 ] ],
                "unknownRecord": {
                    "unknown": "foo",
                    "unknownList": [ "foo", "bar" ],
                    "unknownRecord": { }
                },
                "recordField": {
                    "unknown": "foo",
                    "unknownList": [ "foo", "bar" ],
                    "unknownMatrix": [ [ 1, 2 ], [ 3, 4 ] ],
                    "unknownRecord": {
                        "unknown": "foo",
                        "unknownList": [ "foo", "bar" ],
                        "unknownRecord": { }
                    },
                    "unknownNull": null,
                    "unknownFalse": false,
                    "key": "sampleKey",
                    "val": "sampleVal"
                },
                "stringField": "sample"
            }
            """;

    private final Databinder testee = new Databinder();

    @Test
    void accessField_valid_success() {
        assertEquals("sample", testee.accessField(Sample.class, INSTANCE, "stringField"));
        assertEquals(42, testee.accessField(Sample.class, INSTANCE, "integerField"));
        assertEquals(42L, testee.accessField(Sample.class, INSTANCE, "longField"));
    }

    @Test
    void accessField_validEmpty_success() {
        assertNull(testee.accessField(Sample.class, INSTANCE_EMPTY, "stringField"));
    }

    @Test
    void extractFields_valid_success() {
        Map<String, Object> map = testee.extractFields(Sample.class, INSTANCE);

        assertNotNull(map);
        assertEquals(FIELDS, List.copyOf(map.keySet()));
        assertEquals(MAP.keySet(), map.keySet()); // does not verify order
        assertEquals("sample", map.get("stringField"));
        assertEquals(42, map.get("integerField"));
        assertEquals(42L, map.get("longField"));
    }

    @Test
    void extractFields_validEmpty_success() {
        Map<String, Object> map = testee.extractFields(Sample.class, INSTANCE_EMPTY);

        assertNotNull(map);
        assertEquals(FIELDS, List.copyOf(map.keySet()));
        assertEquals(MAP.keySet(), map.keySet()); // does not verify order
        assertTrue(map.containsKey("stringField"));
        assertNull(map.get("stringField"));
    }

    @Test
    void extractFields_validRecursive_success() {
        SampleTree instance = new SampleTree("A", new SampleTree("B", null));
        Map<String, Object> map = testee.extractFields(SampleTree.class, instance);
        assertEquals("A", map.get("value"));
        assertSame(instance.nested(), map.get("nested"));
    }

    @Test
    void extractFields_notRecord_exception() {
        Exception e = assertThrows(DatabinderException.class, () -> testee.extractFields(SampleClass.class, new SampleClass()));
        assertEquals("Not a record: class com.github.phoswald.rstm.databind.SampleClass", e.getMessage());
    }

    @Test
    void createInstance_valid_success() {
        Sample instance = testee.createInstance(Sample.class, MAP);
        assertEquals(INSTANCE, instance);
    }

    @Test
    void createInstance_validEmpty_success() {
        Sample instance = testee.createInstance(Sample.class, MAP_EMPTY);
        assertEquals(INSTANCE_EMPTY, instance);
    }

    @Test
    void createInstance_validStrings_success() {
        Map<String, Object> map = MAP.entrySet().stream() //
                .map(this::toStringEntry) //
                .collect((Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        Sample instance = testee.createInstance(Sample.class, map);
        assertEquals(INSTANCE, instance);
    }

    private Map.Entry<String, ?> toStringEntry(Map.Entry<String, Object> e) {
        return switch(e.getValue()) {
            case SamplePair pair -> e;
            case List<?> list -> Map.entry(e.getKey(), list.stream().map(this::toStringValue).toList());
            case Object obj -> Map.entry(e.getKey(), obj.toString());
        };
    }

    private Object toStringValue(Object v) {
        return switch(v) {
            case SamplePair pair -> v;
            case Object o -> o.toString();
        };
    }

    @Test
    void createInstance_validBytes_success() {
        Map<String, Object> map = Map.ofEntries( //
                Map.entry("stringField", (byte) 42), //
                Map.entry("integerField", (byte) 42), //
                Map.entry("longField", (byte) 42));
        Sample instance = testee.createInstance(Sample.class, map);
        assertNotNull(instance);
        assertEquals("42", instance.stringField());
        assertEquals(42, instance.integerField());
        assertEquals(42L, instance.longField());
    }

    @Test
    void createInstance_invalidName_exception() {
        Exception e = assertThrows(DatabinderException.class, () -> testee.createInstance(Sample.class, Map.of("badName", "sample")));
        assertEquals("Invalid field for class com.github.phoswald.rstm.databind.Sample: badName", e.getMessage());
    }

    @Test
    void createInstance_notRecord_exception() {
        Exception e = assertThrows(DatabinderException.class, () -> testee.createInstance(SampleClass.class, MAP_EMPTY));
        assertEquals("Not a record: class com.github.phoswald.rstm.databind.SampleClass", e.getMessage());
    }

    @Test
    void toXml_valid_success() {
        String xml = testee.toXml(INSTANCE);
        assertEquals(XML, xml);
    }

    @Test
    void toXml_validEmpty_success() {
        String xml = testee.toXml(INSTANCE_EMPTY);
        assertEquals(XML_EMPTY, xml);
    }

    @Test
    void toXml_validStream_success() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        testee.toXml(INSTANCE, stream);
        assertEquals(XML, new String(stream.toByteArray(), UTF_8));
    }

    @Test
    void toXml_validWriter_success() {
        StringWriter writer = new StringWriter();
        testee.toXml(INSTANCE, writer);
        assertEquals(XML, writer.toString());
    }

    @Test
    void toXml_validNotPretty_success() {
        String xml = testee.pretty(false).toXml(INSTANCE);
        assertEquals(XML.replace("\n", "").replace("    ", ""), xml);
    }

    @Test
    void toJson_valid_success() {
        String json = testee.toJson(INSTANCE);
        assertEquals(JSON, json);
    }

    @Test
    void toJson_validEmpty_success() {
        String json = testee.toJson(INSTANCE_EMPTY);
        assertEquals(JSON_EMPTY, json);
    }

    @Test
    void toJson_validStream_success() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        testee.toJson(INSTANCE, stream);
        assertEquals(JSON, new String(stream.toByteArray(), UTF_8));
    }

    @Test
    void toJson_validWriter_success() {
        StringWriter writer = new StringWriter();
        testee.toJson(INSTANCE, writer);
        assertEquals(JSON, writer.toString());
    }

    @Test
    void toJson_validNotPretty_success() {
        String json = testee.pretty(false).toJson(INSTANCE);
        assertEquals(JSON.replace("\n", "").replace(" ", ""), json);
    }

    @Test
    void fromXml_valid_success() {
        Sample instance = testee.fromXml(XML, Sample.class);
        assertEquals(INSTANCE, instance);
    }

    @Test
    void fromXml_validEmpty_success() {
        String xml = "<sample/>";
        Sample instance = testee.fromXml(xml, Sample.class);
        assertEquals(INSTANCE_EMPTY, instance);
    }

    @Test
    void fromXml_validStream_success() {
        Sample instance = testee.fromXml(new ByteArrayInputStream(XML.getBytes(UTF_8)), Sample.class);
        assertEquals(INSTANCE, instance);
    }

    @Test
    void fromXml_validReader_success() {
        Sample instance = testee.fromXml(new StringReader(XML), Sample.class);
        assertEquals(INSTANCE, instance);
    }

    @Test
    void fromXml_unknownFields_skipped() {
        Sample instance = testee.fromXml(XML_UNKNOWN, Sample.class);
        assertEquals(INSTANCE.stringField(), instance.stringField());
        assertEquals(INSTANCE.recordField(), instance.recordField());
    }

    @Test
    void fromXml_unknownFieldsNotTolerant_rejected() {
        Exception e = assertThrows(DatabinderException.class, () -> testee.tolerant(false).fromXml(XML_UNKNOWN, Sample.class));
        assertEquals("Unknown field for class com.github.phoswald.rstm.databind.Sample: unknown", e.getMessage());
    }

    @Test
    void fromJson_valid_success() {
        Sample instance = testee.fromJson(JSON, Sample.class);
        assertEquals(INSTANCE, instance);
    }

    @Test
    void fromJson_validEmpty_success() {
        String json = "{}";
        Sample instance = testee.fromJson(json, Sample.class);
        assertEquals(INSTANCE_EMPTY, instance);
    }

    @Test
    void fromJson_validStream_success() {
        Sample instance = testee.fromJson(new ByteArrayInputStream(JSON.getBytes(UTF_8)), Sample.class);
        assertEquals(INSTANCE, instance);
    }

    @Test
    void fromJson_validReader_success() {
        Sample instance = testee.fromJson(new StringReader(JSON), Sample.class);
        assertEquals(INSTANCE, instance);
    }

    @Test
    void fromJson_unknownFields_skipped() {
        Sample instance = testee.fromJson(JSON_UNKNOWN, Sample.class);
        assertEquals(INSTANCE.stringField(), instance.stringField());
        assertEquals(INSTANCE.recordField(), instance.recordField());
    }

    @Test
    void fromJson_unknownFieldsNotTolerant_rejected() {
        Exception e = assertThrows(DatabinderException.class, () -> testee.tolerant(false).fromJson(JSON_UNKNOWN, Sample.class));
        assertEquals("Unknown field for class com.github.phoswald.rstm.databind.Sample: unknown", e.getMessage());
    }
}

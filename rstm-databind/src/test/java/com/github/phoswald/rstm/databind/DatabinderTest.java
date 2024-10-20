package com.github.phoswald.rstm.databind;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.phoswald.record.builder.RecordBuilder;

class DatabinderTest {

    private static final Sample INSTANCE = Sample.builder() //
            .stringField("sample") //
            .integerField(42) //
            .longField(42L) //
            .nullField(null) //
            .build();

    private static final String XML = """
            <?xml version="1.0" ?>
            <sample>
                <stringField>sample</stringField>
                <integerField>42</integerField>
                <longField>42</longField>
            </sample>
            """;

    private static final String JSON = """
            {
                "stringField": "sample",
                "integerField": 42,
                "longField": 42
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
        assertEquals(4, map.size());
        assertEquals(List.of("stringField", "integerField", "longField", "nullField"), List.copyOf(map.keySet()));
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
        Map<String, Object> map = Map.ofEntries(Map.entry("stringField", "sample"), Map.entry("integerField", 42));

        Sample instance = testee.create(Sample.class, map);

        assertNotNull(instance);
        assertEquals("sample", instance.stringField());
        assertEquals(42, instance.integerField());
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

        assertNotNull(instance);
        assertEquals("sample", instance.stringField());
        assertEquals(42, instance.integerField());
    }

    @Test
    void fromJson() {
        Sample instance = testee.fromJson(Sample.class, JSON.getBytes(UTF_8));

        assertNotNull(instance);
        assertEquals("sample", instance.stringField());
        assertEquals(42, instance.integerField());
    }

    // XXX: test classes
    // XXX: test collections
    // XXX: test raw types
}

@RecordBuilder
record Sample( //
        String stringField, //
        Integer integerField, //
        Long longField, //
        String nullField //
) {
    static SampleBuilder builder() {
        return new SampleBuilder();
    }
}

class SampleClass { }

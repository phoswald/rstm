package com.github.phoswald.rstm.databind;

import java.util.Map;

import com.github.phoswald.record.builder.RecordBuilder;

@RecordBuilder
public record SampleMap(
        Map<String, String> stringMapField,
        Map<String, SamplePair> recordMapField,
        String stringField
) {
    static SampleMapBuilder builder() {
        return new SampleMapBuilder();
    }
}

package com.github.phoswald.rstm.databind;

import com.github.phoswald.record.builder.RecordBuilder;

@RecordBuilder
record Sample( //
        String stringField, //

        Byte byteField, //
        Short shortField, //
        Integer integerField, //
        Long longField, //
        Float floatField, //
        Double doubleField, //
        Boolean booleanField, //
        Character characterField, //

        byte bytePField, //
        short shortPField, //
        int intPField, //
        long longPField, //
        float floatPField, //
        double doublePField, //
        boolean booleanPField, //
        char charPField, //

        String nullField //
) {
    static SampleBuilder builder() {
        return new SampleBuilder();
    }
}

package com.github.phoswald.rstm.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HttpHeaderValueTest {

    @ParameterizedTest
    @ValueSource(strings = { "text/html", " text/html ", "text/html;charset=utf-8", "text/html ; charset=utf-8 " })
    void valueOnly_valid_success(String value) {
        HttpHeaderValue result = HttpHeaderValue.parse(value);
        assertEquals("text/html", result.valueOnly());
    }

    @ParameterizedTest
    @ValueSource(strings = { "text/html;charset=utf-8", "text/html ; charset=utf-8 ", "v; a=b; charset=utf-8; c=d" })
    void parameter_valid_success(String value) {
        HttpHeaderValue result = HttpHeaderValue.parse(value);
        assertEquals("utf-8", result.parameter("charset").get());
    }

    @ParameterizedTest
    @ValueSource(strings = { "text/html;charset=utf-8;", "text/html;a;b=;charset=utf-8;", "text/html;charset=utf-8;a;b=;" })
    void parameter_invalid_success(String value) {
        HttpHeaderValue result = HttpHeaderValue.parse(value);
        assertEquals("text/html", result.valueOnly());
        assertEquals("utf-8", result.parameter("charset").get());
        assertEquals(1, result.parameters().size());
    }
}

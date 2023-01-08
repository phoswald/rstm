package com.github.phoswald.rstm.http;

import com.github.phoswald.record.builder.RecordBuilder;

@RecordBuilder
public record HttpRequest( //
        HttpMethod method, //
        String path //
) {

    public static HttpRequestBuilder builder() {
        return new HttpRequestBuilder();
    }
}

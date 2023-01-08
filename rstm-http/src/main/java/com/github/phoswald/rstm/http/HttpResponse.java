package com.github.phoswald.rstm.http;

import com.github.phoswald.record.builder.RecordBuilder;

@RecordBuilder
public record HttpResponse( //
        int status, //
        String contentType, //
        byte[] body //
) {

    public static HttpResponseBuilder builder() {
        return new HttpResponseBuilder();
    }

    public static HttpResponse status(int status) {
        return builder().status(status).build();
    }
}

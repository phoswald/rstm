package com.github.phoswald.rstm.http;

import java.nio.charset.StandardCharsets;

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

    public static HttpResponse empty(int status) {
        return builder().status(status).build();
    }

    public static HttpResponse text(int status, String text) {
        return builder() //
                .status(status) //
                .contentType("text/plain") //
                .body(text.getBytes(StandardCharsets.UTF_8)) //
                .build();
    }
}

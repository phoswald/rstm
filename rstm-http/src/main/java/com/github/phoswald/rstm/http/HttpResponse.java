package com.github.phoswald.rstm.http;

import java.nio.charset.StandardCharsets;

import com.github.phoswald.record.builder.RecordBuilder;

@RecordBuilder
public record HttpResponse(
        int status,
        String contentType,
        String location,
        String session,
        byte[] body
) {

    public static HttpResponseBuilder builder() {
        return new HttpResponseBuilder();
    }

    public static HttpResponse empty(int status) {
        return builder().status(status).build();
    }

    public static HttpResponse body(int status, HttpCodec codec, Object body) {
        return builder()
                .status(status)
                .contentType(codec.getContentType())
                .body(codec.encode(body))
                .build();
    }

    public static HttpResponse text(int status, String text) {
        return builder()
                .status(status)
                .contentType("text/plain")
                .body(text.getBytes(StandardCharsets.UTF_8))
                .build();
    }

    public static HttpResponse html(int status, String html) {
        return builder()
                .status(status)
                .contentType("text/html")
                .body(html.getBytes(StandardCharsets.UTF_8))
                .build();
    }

    public static HttpResponse redirect(int status, String location) {
        return builder().status(status).location(location).build();
    }
}

package com.github.phoswald.rstm.http;

import static com.github.phoswald.rstm.http.HttpConstants.CONTENT_TYPE_HTML;
import static com.github.phoswald.rstm.http.HttpConstants.CONTENT_TYPE_TEXT;
import static java.nio.charset.StandardCharsets.UTF_8;

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

    public static HttpResponse text(int status, String text) {
        return builder()
                .status(status)
                .contentType(CONTENT_TYPE_TEXT)
                .body(text.getBytes(UTF_8))
                .build();
    }

    public static HttpResponse html(int status, String html) {
        return builder()
                .status(status)
                .contentType(CONTENT_TYPE_HTML)
                .body(html.getBytes(UTF_8))
                .build();
    }

    public static HttpResponse body(int status, HttpCodec codec, Object body) {
        return builder()
                .status(status)
                .contentType(codec.contentType())
                .body(codec.encode(body))
                .build();
    }

    public static HttpResponse redirect(int status, String location) {
        return builder().status(status).location(location).build();
    }
}

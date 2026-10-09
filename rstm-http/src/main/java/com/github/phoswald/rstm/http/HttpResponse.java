package com.github.phoswald.rstm.http;

import static com.github.phoswald.rstm.http.HttpConstants.CONTENT_TYPE_HTML;
import static com.github.phoswald.rstm.http.HttpConstants.CONTENT_TYPE_TEXT;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.List;

import com.github.phoswald.record.builder.RecordBuilder;
import com.github.phoswald.rstm.http.server.HttpCookie;

@RecordBuilder
public record HttpResponse(
        int status,
        String contentType,
        String location,
        List<HttpCookie> cookies,
        byte[] body
) {

    public static HttpResponseBuilder builder() {
        return new HttpResponseBuilder();
    }

    public HttpResponseBuilder toBuilder() {
        return new HttpResponseBuilder(this);
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
}

package com.github.phoswald.rstm.http;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import com.github.phoswald.record.builder.RecordBuilder;

@RecordBuilder
public record HttpRequest( //
        HttpMethod method, //
        String path, //
        Map<String,String> pathParams, //
        Map<String,String> queryParams, //
        Map<String,String> formParams, //
        byte[] body //
) {

    public static HttpRequestBuilder builder() {
        return new HttpRequestBuilder();
    }

    public HttpRequestBuilder toBuilder() {
        return new HttpRequestBuilder(this);
    }

    public Optional<String> pathParam(String name) {
        return Optional.ofNullable(pathParams.get(name));
    }

    public Optional<String> queryParam(String name) {
        return Optional.ofNullable(queryParams.get(name));
    }

    public Optional<String> formParam(String name) {
        return Optional.ofNullable(formParams.get(name));
    }

    public <T> T body(HttpCodec codec, Class<T> clazz) {
        return body == null ? null : codec.decode(clazz, body);
    }

    public String text() {
        return body == null ? null : new String(body, StandardCharsets.UTF_8); // TODO (http): use correct charset
    }
}

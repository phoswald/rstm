package com.github.phoswald.rstm.http;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import com.github.phoswald.record.builder.RecordBuilder;
import com.github.phoswald.rstm.security.Principal;

@RecordBuilder
public record HttpRequest( //
        HttpMethod method, //
        String path, //
        Map<String,String> pathParams, //
        Map<String,String> queryParams, //
        Map<String,String> formParams, //
        String authorization, //
        String session, //
        Principal principal, //
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
        return body == null ? null : new String(body, StandardCharsets.UTF_8); // TODO: use correct charset
    }

    public String relativizePath(String otherPath) {
        if(!otherPath.startsWith("/")) {
            throw new IllegalArgumentException(otherPath);
        }
        int index = 0;
        int sharedLength = 0;
        while(index < path.length() && index < otherPath.length() && path.charAt(index) == otherPath.charAt(index)) {
            if(path.charAt(index) == '/') {
                sharedLength = index+1;                
            }
            index++;
        }
        otherPath = otherPath.substring(sharedLength);
        index = sharedLength;
        while((index = path.indexOf("/", index)) != -1) {
            otherPath = "../" + otherPath;
            index++;
        }
        return otherPath.isEmpty() ? "." : otherPath;
    }
}

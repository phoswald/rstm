package com.github.phoswald.rstm.http;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import com.github.phoswald.record.builder.RecordBuilder;
import com.github.phoswald.rstm.http.server.HttpServerConfig;
import com.github.phoswald.rstm.security.Principal;

@RecordBuilder
public record HttpRequest(
        HttpServerConfig config,
        HttpMethod method,
        String path,
        Map<String, String> pathParams,
        Map<String, String> queryParams,
        Map<String, String> formParams,
        Map<String, String> cookies,
        String authorization,
        Principal principal,
        byte[] body
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

    public Optional<String> cookie(String name) {
        return Optional.ofNullable(cookies.get(name));
    }

    public <T> T body(HttpCodec codec, Class<T> clazz) {
        return body == null ? null : codec.decode(clazz, body);
    }

    public String text() {
        return body == null ? null : new String(body, StandardCharsets.UTF_8); // TODO (correctness): use correct charset
    }

    public String pathAndQuery() {
        StringBuilder builder = new StringBuilder();
        if(!queryParams.isEmpty()) {
            char separator = '?';
            for(var param : queryParams.entrySet()) {
                builder.append(separator);
                builder.append(param.getKey());
                builder.append('=');
                builder.append(URLEncoder.encode(param.getValue(), UTF_8));
                separator = '&';
            }
        }
        return path + builder.toString();
    }

    public String relativizePath(String otherPath) {
        if (otherPath == null || !otherPath.startsWith("/") || otherPath.contains("//") || otherPath.contains(":")) {
            throw new IllegalArgumentException(otherPath);
        }
        int index = 0;
        int sharedLength = 0;
        while (index < path.length() && index < otherPath.length() && path.charAt(index) == otherPath.charAt(index)) {
            if (path.charAt(index) == '/') {
                sharedLength = index + 1;
            }
            index++;
        }
        otherPath = otherPath.substring(sharedLength);
        index = sharedLength;
        while ((index = path.indexOf("/", index)) != -1) {
            otherPath = "../" + otherPath;
            index++;
        }
        return otherPath.isEmpty() ? "." : otherPath;
    }
}

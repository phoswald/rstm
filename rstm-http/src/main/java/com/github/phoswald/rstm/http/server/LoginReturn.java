package com.github.phoswald.rstm.http.server;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.phoswald.rstm.http.HttpMethod;
import com.github.phoswald.rstm.http.HttpRequest;

/**
 * Remembers the originally requested local URL across the login, in an unsigned cookie validated on read.
 */
class LoginReturn {

    private LoginReturn() { }

    static Optional<HttpCookie> create(HttpRequest request) {
        if (request.method() != HttpMethod.GET) {
            return Optional.empty();
        }
        String url = request.path();
        if (request.queryParams() != null && !request.queryParams().isEmpty()) {
            url += "?" + request.queryParams().entrySet().stream()
                    .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                    .collect(Collectors.joining("&"));
        }
        return Optional.of(HttpCookie.loginReturn(encode(url)));
    }

    static Optional<String> read(HttpRequest request) {
        return request.cookie(HttpCookie.NAME_LOGIN_RETURN)
                .flatMap(LoginReturn::decode)
                .filter(LoginReturn::isValid);
    }

    static String location(HttpRequest request, String returnUrl) {
        String location = request.relativizePath(returnUrl != null && isValid(returnUrl) ? returnUrl : "/");
        // a relative reference like "https:evil.com" would be interpreted as an absolute URL
        int colon = location.indexOf(':');
        if (colon != -1 && location.substring(0, colon).indexOf('/') == -1 && location.substring(0, colon).indexOf('?') == -1) {
            location = "./" + location;
        }
        return location;
    }

    private static boolean isValid(String url) {
        int queryOffset = url.indexOf('?');
        String path = queryOffset == -1 ? url : url.substring(0, queryOffset);
        return path.startsWith("/")
                && !path.contains("//") // could become protocol-relative
                && url.indexOf('\\') == -1
                && url.chars().noneMatch(c -> c < 0x20 || c == 0x7f);
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, UTF_8);
    }

    private static Optional<String> decode(String s) {
        try {
            return Optional.of(URLDecoder.decode(s, UTF_8));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

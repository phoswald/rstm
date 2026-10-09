package com.github.phoswald.rstm.http.server;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.Duration;

import com.github.phoswald.record.builder.RecordBuilder;

@RecordBuilder
public record HttpCookie(
        String name,
        String value,
        boolean httpOnly,
        Duration maxAge,
        SameSite sameSite,
        boolean secure
) {

    public static final String NAME_SESSION = "session";
    public static final String NAME_LOGIN_STATE = "login_state";
    public static final String NAME_LOGIN_RETURN = "login_return";

    public static HttpCookieBuilder builder() {
        return new HttpCookieBuilder();
    }

    public static HttpCookie session(String token) {
        return HttpCookie.builder()
                .name(NAME_SESSION)
                .value(token)
                .httpOnly(true)
                .sameSite(HttpCookie.SameSite.LAX) // must be sent on the redirect back from the provider
                .build();
    }

    public static HttpCookie loginState(String token) {
        return HttpCookie.builder()
                .name(NAME_LOGIN_STATE)
                .value(token)
                .httpOnly(true)
                .maxAge(Duration.ofMinutes(5))
                .sameSite(HttpCookie.SameSite.LAX) // must be sent on the redirect back from the provider
                .build();
    }

    public static HttpCookie loginReturn(String path) {
        return HttpCookie.builder()
                .name(NAME_LOGIN_RETURN)
                .value(encodeValue(path))
                .httpOnly(true)
                .sameSite(HttpCookie.SameSite.LAX) // must be sent on the redirect back from the provider
                .build();
    }

    public static HttpCookie expired(String name) {
        return HttpCookie.builder()
                .name(name)
                .value("")
                .httpOnly(true)
                .maxAge(Duration.ZERO)
                .build();
    }

    public static String encodeValue(String value) {
        return URLEncoder.encode(value, UTF_8); // Uses form encoding (application/x-www-form-urlencoded)
    }

    public static String decodeValue(String value) {
        return URLDecoder.decode(value, UTF_8); // Uses form encoding (application/x-www-form-urlencoded)
    }

    public String toSetCookieHeaderValue() {
        StringBuilder builder = new StringBuilder();
        builder.append(name).append("=").append(value).append("; path=/");
        if(httpOnly) {
            builder.append("; httponly");
        }
        if(maxAge != null) {
            builder.append("; max-age=").append(maxAge.toSeconds());
        }
        if(sameSite != null) {
            builder.append("; samesite=").append(sameSite.name().toLowerCase());
        }
        if(secure) {
            builder.append("; secure");
        }
        return builder.toString();
    }

    public enum SameSite { STRICT, LAX, NONE }
}

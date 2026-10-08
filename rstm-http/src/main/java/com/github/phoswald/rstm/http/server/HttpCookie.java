package com.github.phoswald.rstm.http.server;

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
                .sameSite(HttpCookie.SameSite.STRICT)
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
                .value(path)
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

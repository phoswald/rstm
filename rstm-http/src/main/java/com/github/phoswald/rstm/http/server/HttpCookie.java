package com.github.phoswald.rstm.http.server;

import com.github.phoswald.record.builder.RecordBuilder;

@RecordBuilder
public record HttpCookie(
        String name,
        String value,
        boolean httpOnly,
        SameSite sameSite,
        boolean secure
) {

    public static final String NAME_SESSION = "session";

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

    public String toSetCookieHeaderValue() {
        StringBuilder builder = new StringBuilder();
        builder.append(name).append("=").append(value).append("; path=/");
        if(httpOnly) {
            builder.append("; httponly");
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

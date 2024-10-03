package com.github.phoswald.rstm.security;

import java.util.List;

import com.github.phoswald.record.builder.RecordBuilder;

/*
 * See https://datatracker.ietf.org/doc/html/rfc7519
 */
@RecordBuilder
public record JwtPayload(//
        String iss, // Issuer, optional, string or URI
        String sub, // Subject, optional, string or URI, RSTM: username
        String aud, // Audience, optional, string or URI (single or array)
        Long exp, // Expiration Time, optional, seconds since epoch
        Long nbf, // Not Before, optional, seconds since epoch
        Long iat, // Issued At, optional, seconds since epoch
        List<String> groups // RSTM: roles
) {

    public static JwtPayloadBuilder builder() {
        return new JwtPayloadBuilder();
    }
    
    public JwtPayloadBuilder toBuilder() {
        return new JwtPayloadBuilder(this);
    }

    public static JwtPayload of(String username, List<String> roles) {
        return builder().sub(username).groups(List.copyOf(roles)).build();
    }

    public String username() {
        return sub;
    }

    public List<String> roles() {
        return groups;
    }
}

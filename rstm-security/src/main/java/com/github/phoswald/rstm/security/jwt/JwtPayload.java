package com.github.phoswald.rstm.security.jwt;

import java.util.List;

import com.github.phoswald.record.builder.RecordBuilder;

/**
 * The second of the three parts of a JSON Web Token
 *
 * See https://datatracker.ietf.org/doc/html/rfc7519
 */
@RecordBuilder
public record JwtPayload(
        /**
         * the audience, optional, a string or URI (single or array)
         */
        String aud,
        /**
         * Set by Google and Dex
         */
        String email,
        /**
         * Set by Google and Dex, not set by Microsoft
         */
        Boolean email_verified,
        /**
         * expiration time, optional, seconds since epoch
         */
        Long exp,
        /**
         * Set by RSTM from IDP (roles)
         */
        List<String> groups,
        /**
         * issued at, optional, seconds since epoch
         */
        Long iat,
        /**
         * the issuer, optional, a string or URI
         */
        String iss,
        /**
         * Set by Google and Dex to the real name
         */
        String name,
        /**
         * not before, optional, seconds since epoch
         */
        Long nbf,
        /**
         * Set by Goolge to a image URL (PNG)
         */
        String picture,
        /**
         * the subject, optional, a string or URI, RSTM: username
         */
        String sub //
) {

    public static JwtPayloadBuilder builder() {
        return new JwtPayloadBuilder();
    }

    public JwtPayloadBuilder toBuilder() {
        return new JwtPayloadBuilder(this);
    }

    public static JwtPayload of(String user, List<String> roles) {
        return builder().sub(user).groups(List.copyOf(roles)).build();
    }

    public String determineUser() {
        if (email != null /* && email_verified != null && email_verified.booleanValue() */) {
            return email; // TODO (security hardening): how can be determine unique name (by provider)
        } else {
            return sub;
        }
    }

    public List<String> determineRoles() {
        if (groups != null) {
            return groups;
        } else {
            return List.of();
        }
    }
}

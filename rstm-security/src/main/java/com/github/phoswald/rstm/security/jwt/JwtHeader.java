package com.github.phoswald.rstm.security.jwt;

import com.github.phoswald.record.builder.RecordBuilder;

/**
 * The first of the three parts of a JSON Web Token
 *
 * See https://datatracker.ietf.org/doc/html/rfc7519
 */
@RecordBuilder
public record JwtHeader(
        /**
         * the algorithm, optional, RSTM: must be HS256
         */
        String alg,
        /**
         * the key ID
         */
        String kid,
        /**
         * the type, optional, RSTM: must be JWT
         */
        String typ //
) {

    public static final String TYP_JWT = "JWT";
    public static final String ALG_HS256 = "HS256"; // HMAC with SHA-256 (secret key)
    public static final String ALG_RS256 = "RS256"; // RSA signature with SHA-256 (public key)

    public static JwtHeaderBuilder builder() {
        return new JwtHeaderBuilder();
    }
}

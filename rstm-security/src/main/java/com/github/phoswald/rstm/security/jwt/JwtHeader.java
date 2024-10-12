package com.github.phoswald.rstm.security.jwt;

/**
 * The first of the three parts of a JSON Web Token
 *
 * See https://datatracker.ietf.org/doc/html/rfc7519
 */
public record JwtHeader(
        /**
         * the type, optional, RSTM: must be JWT
         */
        String typ,
        /**
         * the algorithm, optional, RSTM: must be HS256
         */
        String alg,
        /**
         * the key ID
         */
        String kid //
) {
    public static final String TYP_JWT = "JWT";
    public static final String ALG_HS256 = "HS256"; // HMAC with SHA-256 (secret key)
    public static final String ALG_RS256 = "RS256"; // RSA signature with SHA-256 (public key)
}

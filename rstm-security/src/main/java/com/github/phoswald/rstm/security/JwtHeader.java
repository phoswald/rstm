package com.github.phoswald.rstm.security;

/*
 * See https://datatracker.ietf.org/doc/html/rfc7519
 */
public record JwtHeader( //
        String typ, // Type, optional, RSTM: must be JWT
        String alg // Algorithm, optional, RSTM: must be HS256
) {
    public static final String TYP_JWT = "JWT";
    public static final String ALG_HS256 = "HS256";
}

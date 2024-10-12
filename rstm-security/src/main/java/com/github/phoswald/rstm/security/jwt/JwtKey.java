package com.github.phoswald.rstm.security.jwt;

import com.github.phoswald.record.builder.RecordBuilder;

/**
 *  Part of response from JWKS endpoint, see JwtKeySet for root element
 */
@RecordBuilder
public record JwtKey(
        /**
         * the intended use, either sig (signature verification) or enc (encryption)
         */
        String use,
        /**
         * the key type, either RSA or EC (elliptic curve)
         */
        String kty,
        /**
         * the key ID
         */
        String kid,
        /**
         * the algorithm, for example RS256
         */
        String alg,
        /**
         * the modulus for the RSA public key, Base64 URL encoded
         */
        String n,
        /**
         * the exponent for the RSA public key, Base64 URL encoded
         */
        String e //
) { 
    
    public static JwtKeyBuilder builder() {
        return new JwtKeyBuilder();
    }
}

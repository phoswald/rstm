package com.github.phoswald.rstm.security.oidc;

import com.github.phoswald.record.builder.RecordBuilder;

/**
 * Response from well known configuration endpoint
 */
@RecordBuilder
public record Configuration( //
        String issuer, //
        String authorization_endpoint, //
        String token_endpoint, //
        String jwks_uri //
) {

    public ConfigurationBuilder toBuilder() {
        return new ConfigurationBuilder(this);
    }
}

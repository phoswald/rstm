package com.github.phoswald.rstm.security.oidc;

import java.util.Objects;

import com.github.phoswald.record.builder.RecordBuilder;
import com.github.phoswald.rstm.security.jwt.JwtKeySet;

@RecordBuilder
record Provider( //
        String id, //
        String configurationUri, //
        String clientId, //
        String clientSecret, //
        String scopes, //
        Configuration config, // from well known configuration endpoint
        JwtKeySet keySet // from JWKS endpoint
) {
    Provider {
        Objects.requireNonNull(id);
    }

    static ProviderBuilder builder() {
        return new ProviderBuilder();
    }

    ProviderBuilder toBuilder() {
        return new ProviderBuilder(this);
    }
}

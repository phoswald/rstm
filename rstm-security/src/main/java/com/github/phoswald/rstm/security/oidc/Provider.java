package com.github.phoswald.rstm.security.oidc;

import com.github.phoswald.record.builder.RecordBuilder;
import com.github.phoswald.rstm.security.jwt.JwtKeySet;

@RecordBuilder
record Provider( //
        String configurationUri, //
        String clientId, //
        String clientSecret, //
        String scopes, //
        Configuration config, // from well known configuration endpoint
        JwtKeySet keySet // from JWKS endpoint
) {
    
    static ProviderBuilder builder() {
        return new ProviderBuilder();
    }
    
    ProviderBuilder toBuilder() {
        return new ProviderBuilder(this);
    }
}

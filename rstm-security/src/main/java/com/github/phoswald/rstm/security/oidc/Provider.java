package com.github.phoswald.rstm.security.oidc;

import com.github.phoswald.record.builder.RecordBuilder;

@RecordBuilder
record Provider( //
        String configurationUri, //
        String clientId, //
        String clientSecret, //
        String scopes //
) {
    
    static ProviderBuilder builder() {
        return new ProviderBuilder();
    }
}

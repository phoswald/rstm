package com.github.phoswald.rstm.security.oidc;

import com.github.phoswald.record.builder.RecordBuilder;

@RecordBuilder
record ProviderInfo( //
        String configurationEndpoint, //
        String clientId, //
        String clientSecret, //
        String scopes //
) {
    
    static ProviderInfoBuilder builder() {
        return new ProviderInfoBuilder();
    }
}

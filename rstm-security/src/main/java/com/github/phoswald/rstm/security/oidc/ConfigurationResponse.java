package com.github.phoswald.rstm.security.oidc;

import com.github.phoswald.record.builder.RecordBuilder;

@RecordBuilder
public record ConfigurationResponse( //
        String issuer, //
        String authorization_endpoint, //
        String token_endpoint, //
        String jwks_uri //
)  { }

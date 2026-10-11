package com.github.phoswald.rstm.http.server;

import com.github.phoswald.record.builder.RecordBuilder;

@RecordBuilder
record OidcTokenResponse(
        String access_token,
        String issued_token_type,
        String token_type,
        Integer expires_in,
        // String scope,
        String error,
        String error_description
) { }

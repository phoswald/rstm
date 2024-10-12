package com.github.phoswald.rstm.security.oidc;

import com.github.phoswald.record.builder.RecordBuilder;

/**
 * Response from token endpoint
 */
@RecordBuilder
public record Token( //
        String token_type, // typically Bearer
        String id_token, //  A JWT that contains information about the authenticated user
        String access_token, // to be used to access protected resources on behalf of the user
        String refresh_token, // optional
        String scope, // optional, can be set if the requested scopes differ from those granted
        Long expires_in, // lifetime of the access token in seconds
        String error, //
        String error_description //
) { }

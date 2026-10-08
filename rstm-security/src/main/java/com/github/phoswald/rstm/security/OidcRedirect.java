package com.github.phoswald.rstm.security;

/**
 * The result of starting an OIDC login.
 * The state token must be passed back to the callback by the browser (in a cookie)
 */
public record OidcRedirect(String authorizationUrl, String stateToken) { }

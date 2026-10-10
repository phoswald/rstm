package com.github.phoswald.rstm.security;

import java.util.Optional;

/**
 * Top-level interface for authentication
 */
public interface IdentityProvider {

    Optional<Principal> authenticateWithPassword(String username, char[] password);

    Optional<Principal> authenticateWithToken(String token);

    Principal exchangeToken(Principal principal);

    default Optional<OidcRedirect> authenticateWithOidcRedirect(String provider) {
        return Optional.empty();
    }

    default Optional<Principal> authenticateWithOidcCallback(String code, String state, String stateToken) {
        return Optional.empty();
    }
}

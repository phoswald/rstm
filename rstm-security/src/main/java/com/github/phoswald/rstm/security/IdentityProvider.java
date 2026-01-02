package com.github.phoswald.rstm.security;

import java.util.Optional;

/**
 * Top-level interface for authentication
 */
public interface IdentityProvider {

    public default Optional<Principal> authenticateWithPassword(String username, char[] password) {
        return Optional.empty();
    }

    public default Optional<Principal> authenticateWithToken(String token) {
        return Optional.empty();
    }

    public default Optional<String> authenticateWithOidcRedirect(String provider) {
        return Optional.empty();
    }

    public default Optional<Principal> authenticateWithOidcCallback(String code, String state) {
        return Optional.empty();
    }
}

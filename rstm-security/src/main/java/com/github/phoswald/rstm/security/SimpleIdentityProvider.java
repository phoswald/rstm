package com.github.phoswald.rstm.security;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A local IDP that maintains an in-memory set of users, passwords and roles.
 */
public class SimpleIdentityProvider implements IdentityProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TokenProvider tokenProvider;
    private final Map<String, Principal> principals = new HashMap<>();
    private final Map<String, char[]> passwords = new HashMap<>();

    public SimpleIdentityProvider() {
        this(new SimpleTokenProvider());
    }

    public SimpleIdentityProvider(TokenProvider tokenProvider) {
        this.tokenProvider = Objects.requireNonNull(tokenProvider);
    }

    public SimpleIdentityProvider withUser(String username, String password, List<String> roles) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
        principals.put(username, tokenProvider.createPrincipal(username, roles));
        passwords.put(username, password.toCharArray());
        return this;
    }

    @Override
    public Optional<Principal> authenticateWithPassword(String username, char[] password) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
        if (Arrays.equals(password, passwords.get(username))) {
            logger.info("Login successful for username={}", username);
            return Optional.of(principals.get(username));
        } else {
            logger.warn("Login failed for username={}", username);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Principal> authenticateWithToken(String token) {
        return tokenProvider.authenticateWithToken(token);
    }
}

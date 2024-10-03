package com.github.phoswald.rstm.security;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleIdentityProvider extends IdentityProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<String, char[]> passwords = new HashMap<>();
    private final Map<String, Principal> principals = new HashMap<>();

    public SimpleIdentityProvider() {
        super(new SimpleTokenProvider());
    }
    
    public SimpleIdentityProvider add(String username, String password, List<String> roles) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
        Principal principal = createPrincipal(username, roles);
        passwords.put(username, password.toCharArray());
        principals.put(username, principal);
        return this;
    }

    @Override
    public Optional<Principal> authenticate(String username, char[] password) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
        if(Arrays.equals(password, passwords.get(username))) {
            logger.info("Login successful for username={}", username);
            return Optional.of(principals.get(username));
        } else {
            logger.warn("Login failed for username={}", username);
            return Optional.empty();
        }
    }
}

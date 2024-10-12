package com.github.phoswald.rstm.security;

import static com.github.phoswald.rstm.security.Principal.LOCAL_PROVIDER;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A simple token provider that generates random opaque tokens.
 */
public class SimpleTokenProvider implements TokenProvider {

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Principal> tokens = new HashMap<>();

    @Override
    public Principal createPrincipal(String username, List<String> roles) {
        Principal principal = new Principal(username, roles, LOCAL_PROVIDER, createToken());
        tokens.put(principal.token(), principal);
        return principal;

    }

    private String createToken() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    @Override
    public Optional<Principal> authenticateWithToken(String token) {
        Principal principal = tokens.get(token);
        if(principal != null) {
            return Optional.of(principal);
        } else {
            return Optional.empty();
        }
    }
}

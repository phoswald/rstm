package com.github.phoswald.rstm.security;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class IdentityProvider {
    
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Principal> tokens = new HashMap<>();
    
    public abstract Optional<Principal> authenticate(String username, char[] password);
    
    public Optional<Principal> authenticate(String token) {
        Principal principal = tokens.get(token);
        if(principal != null) {
            return Optional.of(principal);
        } else {
            return Optional.empty();
        }
    }
    
    protected Principal createPrincipal(String username, List<String> roles) {
        Principal principal = new Principal(username, roles, createToken());
        tokens.put(principal.token(), principal);
        return principal;
    }
    
    private String createToken() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
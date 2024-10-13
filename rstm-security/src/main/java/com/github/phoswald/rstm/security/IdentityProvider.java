package com.github.phoswald.rstm.security;

import java.util.List;
import java.util.Optional;

public abstract class IdentityProvider {
    
    private final TokenProvider tokenProvider;
    
    public IdentityProvider(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }
    
    protected Principal createPrincipal(String username, List<String> roles) {
        return tokenProvider.createPrincipal(username, roles);
    }
    
    public abstract Optional<Principal> authenticate(String username, char[] password);
    
    public Optional<Principal> authenticate(String token) {
        return tokenProvider.authenticate(token);
    }
    
    public Optional<String> authenticateExtern(String provider) {
        return tokenProvider.authenticateExtern(provider);
    }
    
    public Optional<Principal> authenticateCallback(String code, String state) {
        return tokenProvider.authenticateCallback(code, state);
    }
}

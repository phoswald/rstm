package com.github.phoswald.rstm.security;

import java.util.List;
import java.util.Optional;

public abstract class IdentityProvider {
    
    private final TokenProvider tokenProvider;
    
    public IdentityProvider(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }
    
    public abstract Optional<Principal> authenticate(String username, char[] password);
    
    protected Principal createPrincipal(String username, List<String> roles) {
        return tokenProvider.createPrincipal(username, roles);
    }
    
    public Optional<Principal> authenticate(String token) {
        return tokenProvider.authenticate(token);
    }
    
    
    public Optional<String> authorize(String provider) {
        return tokenProvider.authorize(provider);
    }
    
    public Optional<Principal> callback(String code, String state) {
        return tokenProvider.callback(code, state);
    }
}

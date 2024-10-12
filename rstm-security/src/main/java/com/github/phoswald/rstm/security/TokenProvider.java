package com.github.phoswald.rstm.security;

import java.util.List;
import java.util.Optional;

public interface TokenProvider {
    
    public Principal createPrincipal(String username, List<String> roles);
    
    public default Optional<String> authorize(String provider) {
        return Optional.empty(); // XXX
    }
    
    public default Optional<Principal> callback(String code, String state) {
        return Optional.empty(); // XXX
    }
    
    public Optional<Principal> authenticate(String token);
}

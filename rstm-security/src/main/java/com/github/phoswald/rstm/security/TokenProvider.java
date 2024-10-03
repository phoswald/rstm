package com.github.phoswald.rstm.security;

import java.util.List;
import java.util.Optional;

public interface TokenProvider {
    
    public Principal createPrincipal(String username, List<String> roles);
    
    public Optional<Principal> authenticate(String token);
}

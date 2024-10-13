package com.github.phoswald.rstm.security.oidc;

import java.util.List;
import java.util.Optional;

import com.github.phoswald.rstm.security.Principal;
import com.github.phoswald.rstm.security.TokenProvider;

public class OidcTokenProvider implements TokenProvider {
    
    private final OidcUtil oidcUtil;
    
    public OidcTokenProvider(String redirectUri) {
        this.oidcUtil = new OidcUtil(redirectUri);
    }
    
    public OidcUtil getOidcUtil() {
        return oidcUtil;
    }

    @Override
    public Principal createPrincipal(String username, List<String> roles) {
        throw new UnsupportedOperationException(); // XXX Cannot implement createPrincipal(), refactor !!!
    }

    @Override
    public Optional<Principal> authenticate(String token) {
        return oidcUtil.authenticate(token);
    }

    @Override
    public Optional<String> authenticateExtern(String provider) {
        return oidcUtil.authorize(provider);
    }
    
    @Override
    public Optional<Principal> authenticateCallback(String code, String state) {
        return oidcUtil.callback(code, state);
    }
}

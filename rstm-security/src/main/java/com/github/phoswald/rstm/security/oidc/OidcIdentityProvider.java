package com.github.phoswald.rstm.security.oidc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.phoswald.rstm.security.IdentityProvider;
import com.github.phoswald.rstm.security.Principal;
import com.github.phoswald.rstm.security.jwt.JwtPayload;

/**
 * A federated IDP for OpenID Connect (OIDC). A local upstream is also supported. 
 */
public class OidcIdentityProvider implements IdentityProvider {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final OidcUtil oidcUtil;
    private final IdentityProvider upstream;
    
    public OidcIdentityProvider(String redirectUri, IdentityProvider upstream) {
        this(redirectUri, upstream, Instant::now);
    }
    
    OidcIdentityProvider(String redirectUri, IdentityProvider upstream, Supplier<Instant> clock) {
        this.oidcUtil = new OidcUtil(redirectUri, clock);
        this.upstream = upstream != null ? upstream : new IdentityProvider() { }; 
    }

    public OidcIdentityProvider addDex(String clientId, String clientSecret, String baseUri) {
        oidcUtil.addProvider("dex", Provider.builder() //
                .configurationUri(baseUri + "/.well-known/openid-configuration") //
                .clientId(clientId) //
                .clientSecret(clientSecret) //
                .scopes("openid profile email offline_access") //
                .build());
        return this;
    }
    
    public OidcIdentityProvider addGoogle(String clientId, String clientSecret) {
        oidcUtil.addProvider("google", Provider.builder() //
                .configurationUri("https://accounts.google.com/.well-known/openid-configuration") //
                .clientId(clientId) //
                .clientSecret(clientSecret) //
                .scopes("openid profile email") //
                .build());
        return this;
    }
    
    @Override
    public Optional<Principal> authenticateWithPassword(String username, char[] password) {
        return upstream.authenticateWithPassword(username, password);
    }
    
    @Override
    public Optional<String> authenticateWithOidcRedirect(String provider) {
        return oidcUtil.authenticateWithRedirect(provider);
    }
    
    @Override
    public Optional<Principal> authenticateWithOidcCallback(String code, String state) {
        var result = oidcUtil.authenticateWithCallback(code, state); 
        if (result.isPresent()) {
            return Optional.of(createPrincipal(result.get().payload(), result.get().token()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Principal> authenticateWithToken(String token) {
        Optional<JwtPayload> payload = oidcUtil.validateTokenWithSignature(token);
        if (payload.isPresent()) {
            return Optional.of(createPrincipal(payload.get(), token));
        } else {
            return upstream.authenticateWithToken(token);
        }
    }
    
    private Principal createPrincipal(JwtPayload payload, String token) {
        Principal principal = new Principal(payload.determineUser(), List.of("user"), token); 
        logger.info("Authentication successful for {}", principal.name());
        return principal;
    }
}

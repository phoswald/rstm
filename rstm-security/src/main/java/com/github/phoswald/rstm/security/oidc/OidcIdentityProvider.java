package com.github.phoswald.rstm.security.oidc;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

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
        this(redirectUri, upstream, Instant::now, new SecureRandom());
    }

    OidcIdentityProvider(String redirectUri, IdentityProvider upstream, Supplier<Instant> clock, RandomGenerator random) {
        this.oidcUtil = new OidcUtil(redirectUri, clock, random);
        this.upstream = upstream != null ? upstream : new IdentityProvider() { };
    }

    public OidcIdentityProvider withDex(String clientId, String clientSecret, String baseUri) {
        oidcUtil.addProvider("dex", Provider.builder() //
                .configurationUri(baseUri + "/.well-known/openid-configuration") //
                .clientId(clientId) //
                .clientSecret(clientSecret) //
                .scopes("openid profile email offline_access") //
                .build());
        return this;
    }

    public OidcIdentityProvider withGoogle(String clientId, String clientSecret) {
        oidcUtil.addProvider("google", Provider.builder() //
                .configurationUri("https://accounts.google.com/.well-known/openid-configuration") //
                .clientId(clientId) //
                .clientSecret(clientSecret) //
                .scopes("openid profile email") //
                .build());
        return this;
    }

    public OidcIdentityProvider withMicrosoft(String clientId, String clientSecret, String tenantId) {
        oidcUtil.addProvider("microsoft", Provider.builder() //
                .configurationUri("https://login.microsoftonline.com/" + tenantId + "/v2.0/.well-known/openid-configuration") //
                .clientId(clientId) //
                .clientSecret(clientSecret) //
                .scopes("openid profile email") //
                .build());
        return this;
    }

    public OidcIdentityProvider withFacebook(String clientId, String clientSecret) {
        oidcUtil.addProvider("facebook", Provider.builder() //
                .configurationUri("https://www.facebook.com/.well-known/openid-configuration") //
                .clientId(clientId) //
                .clientSecret(clientSecret) //
                .scopes("email public_profile") //
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

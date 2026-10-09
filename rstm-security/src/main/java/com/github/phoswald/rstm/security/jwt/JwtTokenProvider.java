package com.github.phoswald.rstm.security.jwt;

import static com.github.phoswald.rstm.security.Principal.LOCAL_PROVIDER;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.phoswald.rstm.security.Principal;
import com.github.phoswald.rstm.security.TokenProvider;

public class JwtTokenProvider implements TokenProvider {

    private static final Duration TOKEN_LIFESPAN = Duration.ofHours(2);

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String issuer;
    private final String secret;
    private final JwtUtil jwtUtil;

    public JwtTokenProvider(String site, String secret) {
        this(site, secret, Instant::now);
    }

    JwtTokenProvider(String issuer, String secret, Supplier<Instant> clock) {
        this.issuer = issuer;
        this.secret = secret;
        this.jwtUtil = new JwtUtil(clock);
    }

    @Override
    public Principal createPrincipal(String user, List<String> roles) {
        String token = jwtUtil.createTokenWithHmac(JwtPayload.of(user, roles), issuer, secret, TOKEN_LIFESPAN);
        return new Principal(user, roles, LOCAL_PROVIDER, token);
    }

    @Override
    public Optional<Principal> authenticateWithToken(String token) {
        Optional<JwtValidToken> validToken = jwtUtil.validateTokenWithHmac(token, issuer, secret);
        if (validToken.isPresent()) {
            String username = validToken.get().payload().determineUser();
            List<String> roles = validToken.get().payload().determineRoles();
            logger.debug("Authentication successful for {}", username);
            return Optional.of(new Principal(username, roles, LOCAL_PROVIDER, token));
        } else {
            return Optional.empty();
        }
    }
}

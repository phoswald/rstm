package com.github.phoswald.rstm.security.jwt;

import static com.github.phoswald.rstm.security.Principal.LOCAL_PROVIDER;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.github.phoswald.rstm.security.Principal;
import com.github.phoswald.rstm.security.TokenProvider;

public class JwtTokenProvider implements TokenProvider {

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
        String token = jwtUtil.createTokenWithHmac(JwtPayload.of(user, roles), issuer, secret);
        return new Principal(user, roles, LOCAL_PROVIDER, token);
    }

    @Override
    public Optional<Principal> authenticateWithToken(String token) {
        Optional<JwtValidToken> validToken = jwtUtil.validateTokenWithHmac(token, issuer, secret);
        if (validToken.isPresent()) {
            String user = validToken.get().payload().determineUser();
            List<String> roles = validToken.get().payload().determineRoles();
            return Optional.of(new Principal(user, roles, LOCAL_PROVIDER, token));
        } else {
            return Optional.empty();
        }
    }
}

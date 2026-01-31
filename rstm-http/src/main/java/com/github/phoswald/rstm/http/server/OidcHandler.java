package com.github.phoswald.rstm.http.server;

import static com.github.phoswald.rstm.http.server.HttpServerConfig.get;

import java.util.Optional;

import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;
import com.github.phoswald.rstm.security.Principal;

/**
 * Handles the redirect URI of the OAuth2 authorization code flow for OIDC login
 */
class OidcHandler {

    HttpFilter createRoute() {
        return get(this::handle);
    }

    private HttpResponse handle(HttpRequest request) {
        String code = request.queryParam("code").orElse("");
        String state = request.queryParam("state").orElse("");
        Optional<Principal> principal = request.config().identityProvider().authenticateWithOidcCallback(code, state);
        if (principal.isPresent()) {
            return HttpResponse.builder().status(302).location(request.relativizePath("/")).session(principal.get().token()).build();
        }
        return HttpResponse.builder().status(302).location(request.relativizePath("/login-error.html")).build();
    }
}

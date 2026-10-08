package com.github.phoswald.rstm.http.server;

import static com.github.phoswald.rstm.http.server.HttpServerConfig.get;

import java.util.List;
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
        String stateToken = request.cookie(HttpCookie.NAME_LOGIN_STATE).orElse(null);
        Optional<Principal> principal = request.config().identityProvider().authenticateWithOidcCallback(code, state, stateToken);
        if (principal.isPresent()) {
            return HttpResponse.builder()
                    .status(302)
                    .location(LoginReturn.location(request, LoginReturn.read(request).orElse(null)))
                    .cookies(List.of(
                            HttpCookie.session(principal.get().token()),
                            HttpCookie.expired(HttpCookie.NAME_LOGIN_STATE),
                            HttpCookie.expired(HttpCookie.NAME_LOGIN_RETURN)))
                    .build();
        } else {
            return HttpResponse.builder()
                    .status(302)
                    .location(request.relativizePath("/login-error.html"))
                    .cookies(List.of(HttpCookie.expired(HttpCookie.NAME_LOGIN_STATE)))
                    .build();
        }
    }
}

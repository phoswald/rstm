package com.github.phoswald.rstm.http.server;

import static com.github.phoswald.rstm.http.server.HttpServerConfig.combine;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.get;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.post;

import java.util.List;
import java.util.Optional;

import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;
import com.github.phoswald.rstm.security.OidcRedirect;
import com.github.phoswald.rstm.security.Principal;

/**
 * Handles form based login, either username and password, or triggering an OIDC flow.
 */
class LoginHandler {

    HttpFilter createRoute() {
        return combine(get(this::handle), post(this::handle));
    }

    private HttpResponse handle(HttpRequest request) {
        String provider = request.queryParam("provider").orElse("");
        if (!provider.isEmpty()) {
            Optional<OidcRedirect> redirect = request.config().identityProvider().authenticateWithOidcRedirect(provider);
            if (redirect.isPresent()) {
                return HttpResponse.builder()
                        .status(302)
                        .location(redirect.get().authorizationUrl())
                        .cookies(List.of(HttpCookie.loginState(redirect.get().stateToken())))
                        .build();
            }
        } else {
            String username = request.formParam("username").orElse("");
            char[] password = request.formParam("password").orElse("").toCharArray();
            Optional<Principal> principal = request.config().identityProvider().authenticateWithPassword(username, password);
            if (principal.isPresent()) {
                String returnLocation = request.cookie(HttpCookie.NAME_LOGIN_RETURN).map(HttpCookie::decodeValue).orElse("/");
                return HttpResponse.builder()
                        .status(302)
                        .location(request.relativizePath(returnLocation))
                        .cookies(List.of(
                                HttpCookie.session(principal.get().token()),
                                HttpCookie.expired(HttpCookie.NAME_LOGIN_RETURN)))
                        .build();
            }
        }
        return HttpResponse.builder()
                .status(302)
                .location(request.relativizePath("/login-error.html"))
                .build();
    }
}

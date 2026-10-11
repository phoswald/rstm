package com.github.phoswald.rstm.http.server;

import static com.github.phoswald.rstm.http.server.HttpServerConfig.post;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.route;

import java.util.List;
import java.util.Optional;

import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;
import com.github.phoswald.rstm.security.Principal;

/**
 * Handles form based login, either username and password, or triggering an OIDC flow.
 */
class LoginHandler {

    HttpFilter createRoute() {
        return route("/login", post(this::authenticateWithPassword));
    }

    private HttpResponse authenticateWithPassword(HttpRequest request) {
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
        } else {
            return HttpResponse.builder()
                    .status(302)
                    .location(request.relativizePath("/login-error.html"))
                    .build();
        }
    }
}

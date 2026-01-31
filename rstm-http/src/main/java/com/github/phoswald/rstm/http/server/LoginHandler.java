package com.github.phoswald.rstm.http.server;

import static com.github.phoswald.rstm.http.server.HttpServerConfig.post;

import java.util.Optional;

import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;
import com.github.phoswald.rstm.security.Principal;

/**
 * Handles form based login, either username and password, or triggering an OIDC flow.
 */
class LoginHandler {

    HttpFilter createRoute() {
        return post(this::handle);
    }

    private HttpResponse handle(HttpRequest request) {
        String provider = request.queryParam("provider").orElse("");
        if (!provider.isEmpty()) {
            Optional<String> location = request.config().identityProvider().authenticateWithOidcRedirect(provider);
            if (location.isPresent()) {
                return HttpResponse.builder().status(302).location(location.get()).build();
            }
        } else {
            String username = request.formParam("username").orElse("");
            char[] password = request.formParam("password").orElse("").toCharArray();
            Optional<Principal> principal = request.config().identityProvider().authenticateWithPassword(username, password);
            if (principal.isPresent()) {
                return HttpResponse.builder().status(302).location(request.relativizePath("/")).session(principal.get().token()).build();
            }
        }
        return HttpResponse.builder().status(302).location(request.relativizePath("/login-error.html")).build();
    }
}

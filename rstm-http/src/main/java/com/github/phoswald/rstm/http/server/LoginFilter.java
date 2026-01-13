package com.github.phoswald.rstm.http.server;

import java.util.List;
import java.util.Optional;

import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;
import com.github.phoswald.rstm.security.Principal;

/**
 * Handles form based login, either username and password, or triggering an OIDC flow.
 */
class LoginFilter implements HttpFilter { // TODO (cleanup): should be handler, not a filter

    @Override
    public HttpResponse handle(String path, HttpRequest request, HttpServerConfig config) throws Exception {
        String provider = request.queryParam("provider").orElse("");
        if (!provider.isEmpty()) {
            Optional<String> location = config.identityProvider().authenticateWithOidcRedirect(provider);
            if (location.isPresent()) {
                return HttpResponse.builder().status(302).location(location.get()).build();
            }
        } else {
            String username = request.formParam("username").orElse("");
            char[] password = request.formParam("password").orElse("").toCharArray();
            Optional<Principal> principal = config.identityProvider().authenticateWithPassword(username, password);
            if (principal.isPresent()) {
                return HttpResponse.builder().status(302).location(request.relativizePath("/")).session(principal.get().token()).build();
            }
        }
        return HttpResponse.builder().status(302).location(request.relativizePath("/login-error.html")).build();
    }

    @Override
    public List<RouteMetadata> createMetadata() {
        return List.of();
    }
}

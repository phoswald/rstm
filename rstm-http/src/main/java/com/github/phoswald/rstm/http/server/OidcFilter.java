package com.github.phoswald.rstm.http.server;

import java.util.Optional;

import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;
import com.github.phoswald.rstm.security.Principal;

/**
 * Handles the redirect URI of the OAuth2 authorization code flow for OIDC login
 */
class OidcFilter implements HttpFilter { // TODO (cleanup): should be handler, not a filter

    @Override
    public HttpResponse handle(String path, HttpRequest request, HttpServerConfig config) throws Exception {
        String code = request.queryParam("code").orElse("");
        String state = request.queryParam("state").orElse("");
        Optional<Principal> principal = config.identityProvider().authenticateWithOidcCallback(code, state);
        if (principal.isPresent()) {
            return HttpResponse.builder().status(302).location(request.relativizePath("/")).session(principal.get().token()).build();
        }
        return HttpResponse.builder().status(302).location(request.relativizePath("/login-error.html")).build();
    }
}

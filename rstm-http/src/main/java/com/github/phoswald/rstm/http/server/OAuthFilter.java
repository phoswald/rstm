package com.github.phoswald.rstm.http.server;

import java.util.Optional;

import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;
import com.github.phoswald.rstm.security.Principal;
import com.github.phoswald.rstm.security.oidc.OidcUtil;

public class OAuthFilter implements HttpFilter {

    private final OidcUtil oidc;

    public OAuthFilter(OidcUtil oidc) {
        this.oidc = oidc;
    }

    @Override
    public HttpResponse handle(String path, HttpRequest request, HttpServerConfig config) throws Exception {
        if (path.equals("login")) {
            String provider = request.queryParam("provider").orElse("");
            Optional<String> location = oidc.authorize(provider);
            if(location.isPresent()) {
                return HttpResponse.builder().status(302).location(location.get()).build();
            }

        } else if (path.equals("callback")) {
            String code = request.queryParam("code").orElse("");
            String state = request.queryParam("state").orElse("");
            Optional<Principal> principal = oidc.callback(code, state);
            if (principal.isPresent()) {
                return HttpResponse.builder().status(302).location(request.relativizePath("/")).session(principal.get().token()).build();
            }
        }
        return HttpResponse.builder().status(302).location(request.relativizePath("/login-error.html")).build();
    }
}

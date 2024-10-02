package com.github.phoswald.rstm.http.server;

import java.util.Optional;

import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;
import com.github.phoswald.rstm.security.Principal;
 
class LoginFilter implements HttpFilter {

    @Override
    public HttpResponse handle(String path, HttpRequest request, HttpServerConfig config) throws Exception {
        String username = request.formParam("username").orElse("");
        char[] password = request.formParam("password").orElse("").toCharArray();
        Optional<Principal> principal = config.identityProvider().authenticate(username, password);
        if (principal.isPresent()) {
            return HttpResponse.builder().status(302).location(request.relativizePath("/")).session(principal.get().token()).build();
        } else {
            return HttpResponse.builder().status(302).location(request.relativizePath("/login-error.html")).build();
        }
    }
}

package com.github.phoswald.rstm.http.server;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;
import com.github.phoswald.rstm.security.IdentityProvider;
import com.github.phoswald.rstm.security.Principal;

class AuthFilter implements HttpFilter {

    private final List<String> roles;
    private final HttpFilter filter;

    AuthFilter(List<String> roles, HttpFilter filter) {
        this.roles = roles;
        this.filter = filter;
    }

    @Override
    public HttpResponse handle(String path, HttpRequest request, HttpServerConfig config) throws Exception {
        Optional<Principal> principal = authenticate(request, config.identityProvider());
        if (principal.isEmpty()) {
            return HttpResponse.builder().status(302).location(request.relativizePath("/login.html")).build();
        }
        if(!authorize(principal.get())) {
            return HttpResponse.builder().status(401).build();
        }
        request = request.toBuilder().principal(principal.get()).build();
        return filter.handle(path, request, config);
    }

    private Optional<Principal> authenticate(HttpRequest request, IdentityProvider identityProvider) {
        if (request.authorization() != null) {
            if (request.authorization().toLowerCase().startsWith("basic ")) {
                String authParams = decodeBase64(request.authorization().substring(5).trim());
                int separatorOffset = authParams.indexOf(':');
                if(separatorOffset != -1) {
                    String username = authParams.substring(0, separatorOffset);
                    char[] password = authParams.substring(separatorOffset + 1).toCharArray();
                    return identityProvider.authenticate(username, password);
                }
            }
            if (request.authorization().toLowerCase().startsWith("bearer ")) {
                return identityProvider.authenticate(request.authorization().substring(6).trim());
            }
        }
        if (request.session() != null) {
            return identityProvider.authenticate(request.session());
        }
        return Optional.empty();
    }
    
    private boolean authorize(Principal principal) {
        for(String role: roles) {
            if(principal.roles().contains(role)) {
                return true;
            }
        }
        return false;
    }
    
    private String decodeBase64(String s) {
        try {
            return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
        } catch(IllegalArgumentException e) {
            return "";
        }
    }
}

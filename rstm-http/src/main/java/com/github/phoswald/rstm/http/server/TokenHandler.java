package com.github.phoswald.rstm.http.server;

import static com.github.phoswald.rstm.http.server.HttpServerConfig.post;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import com.github.phoswald.rstm.http.HttpCodec;
import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;
import com.github.phoswald.rstm.http.codec.JsonCodec;
import com.github.phoswald.rstm.security.IdentityProvider;
import com.github.phoswald.rstm.security.Principal;

/**
 * Handles the OAuth2 token exchange
 */
public class TokenHandler {

    private static final String URN_EXCHANGE = "urn:ietf:params:oauth:grant-type:token-exchange";
    private static final String URN_ID_TOKEN = "urn:ietf:params:oauth:token-type:id_token";
    private static final String URN_ACCESS_TOKEN = "urn:ietf:params:oauth:token-type:access_token";

    HttpFilter createRoute() {
        return post(this::handle);
    }

    private HttpResponse handle(HttpRequest request) {
        String grantType = request.formParam("grant_type").orElse(null);
        String subjectToken = request.formParam("subject_token").orElse(null);
        String subjectTokenType = request.formParam("subject_token_type").orElse(null);
        // String requestedTokenType = request.formParam("requested_token_type").orElse(null);
        // String requestedAudience = request.formParam("audience").orElse(null);
        // String requestedScope = request.formParam("scope").orElse(null);

        Token token;
        if(Objects.equals(grantType, URN_EXCHANGE)
                && Objects.equals(subjectTokenType, URN_ID_TOKEN)
                /* && Objects.equals(requestedTokenType, URN_ACCESS_TOKEN) */) {

            Optional<String> internalToken = exchangeToken(request.config(), subjectToken);
            if(internalToken.isPresent()) {
                token = new TokenBuilder()
                        .access_token(internalToken.get())
                        .issued_token_type(URN_ACCESS_TOKEN)
                        .token_type("Bearer")
                        .expires_in((int) Duration.ofHours(20).toSeconds())
                        // .scope("...")
                        .build();
            } else {
                token = new TokenBuilder()
                        .error("invalid_grant")
                        .error_description("Invalid JWT token")
                        .build();
            }
        } else {
            token = new TokenBuilder()
                    .error("invalid_request")
                    .error_description("Invalid parameters grant_type, subject_token_type") /* requested_token_type */
                    .build();
        }

        HttpCodec codec = JsonCodec.json();
        return HttpResponse.body(token.access_token() != null ? 200 : 400, codec, token);
    }

    private Optional<String> exchangeToken(HttpServerConfig config, String token) {
        IdentityProvider identityProvider = config.identityProvider();
        return identityProvider.authenticateWithToken(token)
                .map(identityProvider::exchangeToken)
                .map(Principal::token);
    }
}

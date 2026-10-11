package com.github.phoswald.rstm.http.server;

import static com.github.phoswald.rstm.http.server.HttpServerConfig.combine;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.get;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.post;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.route;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.github.phoswald.rstm.http.HttpCodec;
import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;
import com.github.phoswald.rstm.http.codec.JsonCodec;
import com.github.phoswald.rstm.security.OidcRedirect;
import com.github.phoswald.rstm.security.Principal;

/**
 * Handles the redirect and callback of the OAuth2 authorization code flow for OIDC login; OAuth2 token exchange
 */
class OidcHandler {

    private static final String URN_EXCHANGE = "urn:ietf:params:oauth:grant-type:token-exchange";
    private static final String URN_ID_TOKEN = "urn:ietf:params:oauth:token-type:id_token";
    private static final String URN_ACCESS_TOKEN = "urn:ietf:params:oauth:token-type:access_token";

    HttpFilter createRoute() {
        return route("/oauth", combine(
                route("/redirect", get(this::authenticateWithOidcRedirect)),
                route("/callback", get(this::authenticateWithOidcCallback)),
                route("/token", post(this::exchangeToken))));
    }

    private HttpResponse authenticateWithOidcRedirect(HttpRequest request) {
        String provider = request.queryParam("provider").orElse("");
        Optional<OidcRedirect> redirect = request.config().identityProvider().authenticateWithOidcRedirect(provider);
        if (redirect.isPresent()) {
            return HttpResponse.builder()
                    .status(302)
                    .location(redirect.get().authorizationUrl())
                    .cookies(List.of(HttpCookie.loginState(redirect.get().stateToken())))
                    .build();
        } else {
            return HttpResponse.builder()
                    .status(302)
                    .location(request.relativizePath("/login-error.html"))
                    .build();
        }
    }

    private HttpResponse authenticateWithOidcCallback(HttpRequest request) {
        String code = request.queryParam("code").orElse("");
        String state = request.queryParam("state").orElse("");
        String stateToken = request.cookie(HttpCookie.NAME_LOGIN_STATE).orElse(null);
        String returnLocation = request.cookie(HttpCookie.NAME_LOGIN_RETURN).map(HttpCookie::decodeValue).orElse("/");
        Optional<Principal> principal = request.config().identityProvider().authenticateWithOidcCallback(code, state, stateToken);
        if (principal.isPresent()) {
            return HttpResponse.builder()
                    .status(302)
                    .location(request.relativizePath(returnLocation))
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

    private HttpResponse exchangeToken(HttpRequest request) {
        String grantType = request.formParam("grant_type").orElse(null);
        String subjectToken = request.formParam("subject_token").orElse(null);
        String subjectTokenType = request.formParam("subject_token_type").orElse(null);
        // String requestedTokenType = request.formParam("requested_token_type").orElse(null);
        // String requestedAudience = request.formParam("audience").orElse(null);
        // String requestedScope = request.formParam("scope").orElse(null);

        OidcTokenResponse response;
        if(Objects.equals(grantType, URN_EXCHANGE)
                && Objects.equals(subjectTokenType, URN_ID_TOKEN)
            /* && Objects.equals(requestedTokenType, URN_ACCESS_TOKEN) */) {

            Optional<String> internalToken = request.config().identityProvider().authenticateWithToken(subjectToken)
                    .map(request.config().identityProvider()::exchangeToken)
                    .map(Principal::token);
            if(internalToken.isPresent()) {
                response = new OidcTokenResponseBuilder()
                        .access_token(internalToken.get())
                        .issued_token_type(URN_ACCESS_TOKEN)
                        .token_type("Bearer")
                        .expires_in((int) Duration.ofHours(20).toSeconds())
                        // .scope("...")
                        .build();
            } else {
                response = new OidcTokenResponseBuilder()
                        .error("invalid_grant")
                        .error_description("Invalid JWT token")
                        .build();
            }
        } else {
            response = new OidcTokenResponseBuilder()
                    .error("invalid_request")
                    .error_description("Invalid parameters grant_type, subject_token_type") /* requested_token_type */
                    .build();
        }

        HttpCodec codec = JsonCodec.json();
        return HttpResponse.body(response.access_token() != null ? 200 : 400, codec, response);
    }
}

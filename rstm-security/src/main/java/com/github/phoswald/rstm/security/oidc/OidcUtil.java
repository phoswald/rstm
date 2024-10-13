package com.github.phoswald.rstm.security.oidc;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.phoswald.rstm.security.jwt.JwtKeySet;
import com.github.phoswald.rstm.security.jwt.JwtPayload;
import com.github.phoswald.rstm.security.jwt.JwtUtil;

class OidcUtil {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final StateManager stateManager = new StateManager();
    private final JwtUtil jwtUtil;
    private final Jsonb json = JsonbBuilder.create();

    private final String redirectUri;
    private final Map<String, Provider> providers = new LinkedHashMap<>();

    OidcUtil(String redirectUri, Supplier<Instant> clock) {
        this.redirectUri = redirectUri;
        this.jwtUtil = new JwtUtil(clock);
    }

    void addProvider(String providerId, Provider provider) {
        Configuration config = request(provider.configurationUri(), Configuration.class, null);
        if (config == null) {
            throw new IllegalArgumentException("Failed to load " + provider.configurationUri()); // TODO (observability) propagate case
        }

        JwtKeySet keySet = request(config.jwks_uri(), JwtKeySet.class, null);
        if (keySet == null) {
            throw new IllegalArgumentException("Failed to load " + config.jwks_uri()); // TODO (observability) propagate case
        }
        
        providers.put(providerId, provider.toBuilder().config(config).keySet(keySet).build());
    }

    Optional<String> authenticateWithRedirect(String providerId) {
        logger.info("Starting athentication with redirect for provider={}", providerId);

        Provider provider = providers.get(providerId);
        if (provider == null) {
            logger.warn("Provider not found: {}", providerId);
            return Optional.empty();
        }

        String state = stateManager.create(provider);
        String query = query(List.of( //
                Map.entry("response_type", "code"), //
                Map.entry("client_id", provider.clientId()), //
                Map.entry("redirect_uri", this.redirectUri), //
                Map.entry("scope", provider.scopes()), //
                Map.entry("state", state)));
        return Optional.of(provider.config().authorization_endpoint() + "?" + query);
    }

    Optional<TokenAndPayload> authenticateWithCallback(String code, String state) {
        logger.info("Completing authentication with callback for code={}, state={}", code, state);

        State stateObj = stateManager.consume(state);
        if (stateObj == null) {
            logger.warn("State invalid or already consumed: {}", state);
            return Optional.empty();
        }

        Provider provider = stateObj.provider();
        Token token = getTokenFromCode(code, provider);
        if (token == null) {
            return Optional.empty();
        }
        if (token.error() != null) {
            logger.info("Token endpoint failed: error={}, error_description={}", token.error(),
                    token.error_description());
            return Optional.empty();
        }

        return jwtUtil.validateTokenWithSignature(
                token.id_token(), provider.config().issuer(), provider.clientId(), provider.keySet()) //
                .map(payload -> new TokenAndPayload(token.id_token(), payload));
    }

    Optional<JwtPayload> validateTokenWithSignature(String token) {
        for(Provider provider : providers.values()) {
            // TODO (optimize): decode token only once!
            Optional<JwtPayload> payload = jwtUtil.validateTokenWithSignature(
                    token, provider.config().issuer(), provider.clientId(), provider.keySet());
            if(payload.isPresent()) {
                return payload;
            }
        }
        return Optional.empty();
    }

    private Token getTokenFromCode(String code, Provider provider) {
        String query = query(List.of( //
                Map.entry("grant_type", "authorization_code"), //
                Map.entry("code", code), //
                Map.entry("client_id", provider.clientId()), //
                Map.entry("client_secret", provider.clientSecret()), //
                Map.entry("redirect_uri", this.redirectUri)));
        return request(provider.config().token_endpoint(), Token.class, query);
    }

    private String query(List<Map.Entry<String, String>> params) {
        return String.join("&", params.stream() //
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), UTF_8)) //
                .toList());
    }

    private <T> T request(String uri, Class<T> responseClass, String requestBody) {
        String method = requestBody == null ? "GET" : "POST";
        try {
            HttpURLConnection connection = (HttpURLConnection) new URI(uri).toURL().openConnection();
            connection.setRequestMethod(method);
            if (requestBody != null) {
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setDoOutput(true);
                try (OutputStream requestStream = connection.getOutputStream()) {
                    requestStream.write(requestBody.toString().getBytes(UTF_8));
                    requestStream.flush();
                }
            }
            int responseCode = connection.getResponseCode();
            String responseBody;
            if (responseCode < 200 || responseCode >= 300) {
                logger.warn("{} {} failed: {}", method, uri, responseCode);
                logger.debug(requestBody);
                return null; // TODO (observability) propagate case as IOException
            }
            try (InputStream responseStream = connection.getInputStream()) {
                responseBody = new String(responseStream.readAllBytes(), UTF_8);
            }
            logger.info("{} {} succeeded: {}", method, uri, responseCode);
            logger.debug(requestBody);
            logger.debug(responseBody);
            if (responseClass == String.class) {
                return responseClass.cast(responseBody);
            } else {
                return json.fromJson(responseBody, responseClass);
            }
        } catch (IOException | URISyntaxException e) {
            logger.error("{} {} failed:", method, uri, e);
            logger.debug(requestBody);
            return null;
        }
    }

    static record TokenAndPayload(String token, JwtPayload payload) { }
}

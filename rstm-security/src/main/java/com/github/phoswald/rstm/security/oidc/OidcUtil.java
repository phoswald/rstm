package com.github.phoswald.rstm.security.oidc;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.phoswald.rstm.databind.Databinder;
import com.github.phoswald.rstm.security.jwt.JwtKeySet;
import com.github.phoswald.rstm.security.jwt.JwtUtil;
import com.github.phoswald.rstm.security.jwt.JwtValidToken;

class OidcUtil {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Databinder binder = new Databinder();
    private final StateManager stateManager;
    private final JwtUtil jwtUtil;

    private final String redirectUri;
    private final Map<String, Provider> providers = new LinkedHashMap<>();

    OidcUtil(String redirectUri, Supplier<Instant> clock, RandomGenerator random) {
        this.redirectUri = redirectUri;
        this.stateManager = new StateManager(Duration.ofMinutes(5), clock, random);
        this.jwtUtil = new JwtUtil(clock);
    }

    void addProvider(Provider provider) {
        try {
            Configuration config = request(provider.configurationUri(), Configuration.class, null);
            JwtKeySet keySet = request(config.jwks_uri(), JwtKeySet.class, null);
            if(provider.id().equals("facebook")) {
                config = config.toBuilder()
                        .token_endpoint("https://graph.facebook.com/v21.0/oauth/access_token")
                        .build();
            }
            providers.put(provider.id(), provider.toBuilder().config(config).keySet(keySet).build());
        } catch(IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }

    Optional<String> authenticateWithRedirect(String providerId) {
        logger.info("Starting athentication with redirect for provider={}", providerId);

        Provider provider = providers.get(providerId);
        if (provider == null) {
            logger.warn("Provider not found: {}", providerId);
            return Optional.empty();
        }

        String state = stateManager.create(provider);
        String query = query(List.of(
                Map.entry("response_type", "code"),
                Map.entry("client_id", provider.clientId()),
                Map.entry("redirect_uri", this.redirectUri),
                Map.entry("scope", provider.scopes()),
                Map.entry("state", state)));
        return Optional.of(provider.config().authorization_endpoint() + "?" + query);
    }

    Optional<JwtValidToken> authenticateWithCallback(String code, String state) {
        logger.info("Completing authentication with callback with code={}, state={}", code, state);

        State stateObj = stateManager.consume(state);
        if (stateObj == null) {
            logger.warn("State invalid or already consumed: {}", state);
            return Optional.empty();
        }

        Provider provider = stateObj.provider();
        Token token;
        try {
            String query = query(List.of(
                    Map.entry("grant_type", "authorization_code"),
                    Map.entry("code", code),
                    Map.entry("client_id", provider.clientId()),
                    Map.entry("client_secret", provider.clientSecret()),
                    Map.entry("redirect_uri", this.redirectUri)));
            token = request(provider.config().token_endpoint(), Token.class, query);
            if (token.error() != null) {
                throw new IOException(String.format(
                        "Received error=%s, error_description=%s", token.error(), token.error_description()));
            }
        } catch(IOException e) {
            logger.error("Failed to get token from code: {}", e.getMessage());
            return Optional.empty();
        }

        return jwtUtil.validateTokenWithRsa(
                token.id_token(), provider.config().issuer(), provider.clientId(), provider.id(), provider.keySet());
    }

    Optional<JwtValidToken> validateToken(String token) {
        // TODO (optimize): decode token only once!
        for(Provider provider : providers.values()) {
            Optional<JwtValidToken> validToken = jwtUtil.validateTokenWithRsa(
                    token, provider.config().issuer(), provider.clientId(), provider.id(), provider.keySet());
            if(validToken.isPresent()) {
                return validToken;
            }
        }
        return Optional.empty();
    }

    private String query(List<Map.Entry<String, String>> params) {
        return String.join("&", params.stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), UTF_8))
                .toList());
    }

    private <T> T request(String uri, Class<T> responseClass, String requestBody) throws IOException {
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
                throw new IOException("Received status " + responseCode);
            }
            try (InputStream responseStream = connection.getInputStream()) {
                responseBody = new String(responseStream.readAllBytes(), UTF_8);
            }
            logger.info("{} {} succeeded", method, uri);
            logger.debug(requestBody);
            logger.debug(responseBody);
            if (responseClass == String.class) {
                return responseClass.cast(responseBody);
            } else {
                return binder.fromJson(responseBody, responseClass);
            }
        } catch (IOException | URISyntaxException e) {
            String message = String.format("%s %s failed: %s", method, uri, e.getMessage());
            logger.warn(message);
            logger.debug(requestBody);
            throw new IOException(message);
        }
    }
}

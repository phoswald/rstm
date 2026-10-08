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
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.phoswald.rstm.databind.Databinder;
import com.github.phoswald.rstm.security.OidcRedirect;
import com.github.phoswald.rstm.security.jwt.JwtKeySet;
import com.github.phoswald.rstm.security.jwt.JwtPayload;
import com.github.phoswald.rstm.security.jwt.JwtUtil;
import com.github.phoswald.rstm.security.jwt.JwtValidToken;

class OidcUtil {

    /**
     * Issuer and audience of the OIDC flow cookie. Distinct from any issuer used for local tokens,
     * so the flow cookie is never accepted as a session token even if the same secret is used.
     */
    private static final String STATE_ISSUER = "rstm-security-login-state";
    private static final Duration STATE_LIFESPAN = Duration.ofMinutes(5);

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Databinder binder = new Databinder();
    private final RandomGenerator random;
    private final JwtUtil jwtUtil;

    private final String redirectUri;
    private final String secret;
    private final Map<String, Provider> providers = new LinkedHashMap<>();

    OidcUtil(String redirectUri, String secret, Supplier<Instant> clock, RandomGenerator random) {
        this.redirectUri = Objects.requireNonNull(redirectUri);
        this.secret = Objects.requireNonNull(secret);
        this.random = random;
        this.jwtUtil = new JwtUtil(clock);
    }

    void addProvider(Provider provider) {
        try {
            Configuration config = request(provider.configurationUri(), Configuration.class, null);
            JwtKeySet keySet = request(config.jwks_uri(), JwtKeySet.class, null);
            if (provider.id().equals("facebook")) {
                config = config.toBuilder()
                        .token_endpoint("https://graph.facebook.com/v21.0/oauth/access_token")
                        .build();
            }
            providers.put(provider.id(), provider.toBuilder().config(config).keySet(keySet).build());
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }

    Optional<OidcRedirect> authenticateWithRedirect(String providerId) {
        logger.info("Starting athentication with redirect for provider={}", providerId);

        Provider provider = providers.get(providerId);
        if (provider == null) {
            logger.warn("Provider not found: {}", providerId);
            return Optional.empty();
        }

        String state = createState();
        JwtPayload statePayload = JwtPayload.builder()
                .login_state(state)
                .login_provider(provider.id())
                .build();
        String stateToken = jwtUtil.createTokenWithHmac(statePayload, STATE_ISSUER, secret, STATE_LIFESPAN);
        String query = query(List.of(
                Map.entry("response_type", "code"),
                Map.entry("client_id", provider.clientId()),
                Map.entry("redirect_uri", this.redirectUri),
                Map.entry("scope", provider.scopes()),
                Map.entry("state", state)));
        return Optional.of(new OidcRedirect(provider.config().authorization_endpoint() + "?" + query, stateToken));
    }

    Optional<JwtValidToken> authenticateWithCallback(String code, String state, String stateToken) {
        logger.info("Completing authentication with callback.");
        logger.debug("code={}, state={}", code, state);

        if (stateToken == null) {
            logger.warn("State token missing.");
            return Optional.empty();
        }
        Optional<JwtValidToken> validStateToken = jwtUtil.validateTokenWithHmac(stateToken, STATE_ISSUER, secret);
        if (validStateToken.isEmpty()) {
            logger.warn("State token invalid or expired.");
            return Optional.empty();
        }
        JwtPayload statePayload = validStateToken.get().payload();
        if (state == null || statePayload.login_state() == null || !Objects.equals(state, statePayload.login_state())) {
            logger.warn("State mismatch.");
            logger.debug("state={}, state from token={}", state, statePayload.login_state());
            return Optional.empty();
        }

        Provider provider = providers.get(statePayload.login_provider());
        if (provider == null) {
            logger.warn("Provider not found: {}", statePayload.login_provider());
            return Optional.empty();
        }
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
        } catch (IOException e) {
            logger.error("Failed to get token from code: {}", e.getMessage());
            return Optional.empty();
        }

        return jwtUtil.validateTokenWithRsa(
                token.id_token(), provider.config().issuer(), provider.clientId(), provider.id(), provider.keySet());
    }

    Optional<JwtValidToken> validateToken(String token) {
        // TODO (optimize): decode token only once!
        for (Provider provider : providers.values()) {
            Optional<JwtValidToken> validToken = jwtUtil.validateTokenWithRsa(
                    token, provider.config().issuer(), provider.clientId(), provider.id(), provider.keySet());
            if (validToken.isPresent()) {
                return validToken;
            }
        }
        return Optional.empty();
    }

    private String createState() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
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

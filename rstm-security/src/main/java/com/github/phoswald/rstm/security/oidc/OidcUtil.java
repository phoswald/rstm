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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.phoswald.rstm.security.Principal;
import com.github.phoswald.rstm.security.jwt.JwtKeySet;
import com.github.phoswald.rstm.security.jwt.JwtPayload;
import com.github.phoswald.rstm.security.jwt.JwtUtil;

public class OidcUtil {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final StateManager stateManager = new StateManager(); 
    private final JwtUtil jwtUtil = new JwtUtil(Instant::now);
    private final Jsonb json = JsonbBuilder.create();

    private final String redirectUri;

    private final Map<String, ProviderInfo> providers = new HashMap<>();
    
    public OidcUtil(String redirectUri) {
        this.redirectUri = redirectUri;
    }
    
    public OidcUtil addDex(String clientId, String clientSecret, String baseUri) {
        providers.put("dex", ProviderInfo.builder() //
                .configurationEndpoint(baseUri + "/.well-known/openid-configuration") //
                .clientId(clientId) //
                .clientSecret(clientSecret) //
                .scopes("openid profile email offline_access") //
                .build());
        return this;
    }
    
    public OidcUtil addGoogle(String clientId, String clientSecret) {
        providers.put("google", ProviderInfo.builder() //
                .configurationEndpoint("https://accounts.google.com/.well-known/openid-configuration") //
                .clientId(clientId) //
                .clientSecret(clientSecret) //
                .scopes("openid profile email") //
                .build());
        return this;
    }
    
    public Optional<String> authorize(String provider) {
        logger.info("Handling authorize for provider={}", provider);

        ProviderInfo providerInfo = providers.get(provider);
        if (providerInfo == null) {
            logger.warn("Provider not found: {}", provider);
            return Optional.empty();
        }

        ConfigurationResponse config = request(providerInfo.configurationEndpoint(), ConfigurationResponse.class, null);
        if (config == null) {
            return Optional.empty();
        }
        
        JwtKeySet keySet = request(config.jwks_uri(), JwtKeySet.class, null);
        if (keySet == null) {
            return Optional.empty();
        }
        
        String state = stateManager.create(providerInfo, config, keySet);
        String query = query(List.of( //
                Map.entry("response_type", "code"), //
                Map.entry("client_id", providerInfo.clientId()), //
                Map.entry("redirect_uri", this.redirectUri), //
                Map.entry("scope", providerInfo.scopes()), //
                Map.entry("state", state)));
        return Optional.of(config.authorization_endpoint() + "?" + query);
    }

    public Optional<Principal> callback(String code, String state) {
        logger.info("Handling callback for code={}, state={}", code, state);
        
        StateInfo stateInfo = stateManager.consume(state);
        if (stateInfo == null) {
            logger.warn("State invalid or already consumed: {}", state);
            return Optional.empty();
        }
        
        TokenResponse token = getTokenFromCode(code, stateInfo.providerInfo(), stateInfo.config());
        if (token == null) {
            return Optional.empty();
        }
        if (token.error() != null) {
            logger.info("Token endpoint returned error={}, error_description={}", token.error(), token.error_description());
            return Optional.empty();
        }
        
        Optional<JwtPayload> payload = jwtUtil.validateTokenWithSignature(token.id_token(), // 
                stateInfo.config().issuer(), stateInfo.providerInfo().clientId(), stateInfo.keySet());
        if(payload.isEmpty()) {
            logger.warn("ID token not valid: {}", token.id_token());
            return Optional.empty();
        }
        
        Principal principal = new Principal(payload.get().determineUser(), payload.get().determineRoles(), token.id_token()); 
        logger.info("Login successful for {}, token={}", principal.name(), principal.token());
        return Optional.of(principal);
    }

    private TokenResponse getTokenFromCode(String code, ProviderInfo providerInfo, ConfigurationResponse config) {
        String query = query(List.of( //
                Map.entry("grant_type", "authorization_code"), //
                Map.entry("code", code), //
                Map.entry("client_id", providerInfo.clientId()), //
                Map.entry("client_secret", providerInfo.clientSecret()), //
                Map.entry("redirect_uri", this.redirectUri)));
        return request(config.token_endpoint(), TokenResponse.class, query);
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
                return null;
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
}

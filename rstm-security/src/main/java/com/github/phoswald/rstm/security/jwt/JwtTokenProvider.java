package com.github.phoswald.rstm.security.jwt;

import static com.github.phoswald.rstm.security.jwt.JwtHeader.ALG_HS256;
import static com.github.phoswald.rstm.security.jwt.JwtHeader.TYP_JWT;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.phoswald.rstm.security.Principal;
import com.github.phoswald.rstm.security.TokenProvider;

public class JwtTokenProvider implements TokenProvider {

    private static final Pattern TOKEN_PATTERN = Pattern.compile( //
            "([A-Z-a-z0-9_-]+)\\.([A-Z-a-z0-9_-]+)\\.([A-Z-a-z0-9_-]+)");

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Jsonb json = JsonbBuilder.create();
    private final String issuer;
    private final String secret;
    private final LongSupplier clock;
    private final long clockSkew = Duration.ofMinutes(1).toSeconds();
    private final long lifespan = Duration.ofHours(2).toSeconds();

    public JwtTokenProvider(String site, String secret) {
        this(site, secret, Instant::now);
    }

    JwtTokenProvider(String issuer, String secret, Supplier<Instant> clock) {
        this.issuer = Objects.requireNonNull(issuer, "Parameter issuer must not be null");
        this.secret = secret;
        this.clock = () -> clock.get().toEpochMilli() / 1000;
    }

    @Override
    public Principal createPrincipal(String username, List<String> roles) {
        String token = createTokenWithHmac(JwtPayload.of(username, roles));
        return new Principal(username, roles, token);
    }

    @Override
    public Optional<Principal> authenticate(String token) {
        Optional<JwtPayload> payload = validateTokenWithHmac(token);
        if(payload.isPresent()) {
            return Optional.of(new Principal(payload.get().username(), payload.get().roles(), token));
        } else {
            return Optional.empty();
        }
    }

    private String createTokenWithHmac(JwtPayload payload) {
        Objects.requireNonNull(payload, "Parameter payload must not be null");
        Objects.requireNonNull(secret, "Parameter secret must not be null");
        long now = clock.getAsLong();
        long exp = now + lifespan;
        String payloadStr = json.toJson(payload.toBuilder().iss(issuer).aud(issuer).iat(now).nbf(now).exp(exp).build());
        String headerStr = json.toJson(new JwtHeader(TYP_JWT, ALG_HS256));
        byte[] signature = createHmac(encodeBase64(headerStr) + "." + encodeBase64(payloadStr), secret);
        return encodeBase64(headerStr) + "." + encodeBase64(payloadStr) + "." + encodeBase64(signature);
    }

    private Optional<JwtPayload> validateTokenWithHmac(String token) {
        Objects.requireNonNull(token, "Parameter token must not be null");
        Objects.requireNonNull(secret, "Parameter secret must not be null");
        Matcher matcher = TOKEN_PATTERN.matcher(token);
        if (!matcher.matches()) {
            logger.warn("Token has invalid format");
            return Optional.empty();
        }
        String headerStr = decodeBase64toString(matcher.group(1));
        String payloadStr = decodeBase64toString(matcher.group(2));
        byte[] signature = decodeBase64(matcher.group(3));
        byte[] signatureExpected = createHmac(encodeBase64(headerStr) + "." + encodeBase64(payloadStr), secret);
        if (!Arrays.equals(signature, signatureExpected)) {
            logger.warn("Token signature mismatch");
            return Optional.empty();
        }
        JwtHeader header = json.fromJson(headerStr, JwtHeader.class);
        if (!Objects.equals(header.typ(), TYP_JWT) || !Objects.equals(header.alg(), ALG_HS256)) {
            logger.warn("Token header unsupported: typ={}, alg={}", header.typ(), header.alg());
            return Optional.empty();
        }
        JwtPayload payload = json.fromJson(payloadStr, JwtPayload.class);
        if (!Objects.equals(payload.iss(), issuer) || !Objects.equals(payload.aud(), issuer)) {
            logger.warn("Token issuer or audience mismatch: iss={}, aud={}", payload.iss(), payload.aud());
            return Optional.empty();
        }
        long now = clock.getAsLong();
        if(payload.nbf() != null && payload.nbf() - clockSkew > now) {
            logger.warn("Token not yet valid: nbf={}", payload.nbf());
            return Optional.empty();
        }
        if(payload.exp() != null && payload.exp() + clockSkew < now) {
            logger.warn("Token has expired: exp={}", payload.exp());
            return Optional.empty();
        }
        return Optional.of(payload);
    }

    private byte[] createHmac(String data, String secret) {
        try {
            String algorithm = "HmacSHA256";
            Key key = new SecretKeySpec(secret.getBytes(UTF_8), algorithm);
            Mac mac = Mac.getInstance(algorithm);
            mac.init(key);
            mac.update(data.getBytes(UTF_8));
            return mac.doFinal();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute HMAC with SHA-256", e);
        }
    }

    private String encodeBase64(String data) {
        return encodeBase64(data.getBytes(UTF_8));
    }

    private String encodeBase64(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private String decodeBase64toString(String data) {
        return new String(decodeBase64(data), UTF_8);
    }

    private byte[] decodeBase64(String data) {
        return Base64.getUrlDecoder().decode(data);
    }
}

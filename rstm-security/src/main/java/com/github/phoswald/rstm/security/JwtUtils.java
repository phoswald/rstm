package com.github.phoswald.rstm.security;

import static com.github.phoswald.rstm.security.JwtHeader.ALG_HS256;
import static com.github.phoswald.rstm.security.JwtHeader.TYP_JWT;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

class JwtUtils {

    private static final Pattern TOKEN_PATTERN = Pattern.compile( //
            "([A-Z-a-z0-9_-]+)\\.([A-Z-a-z0-9_-]+)\\.([A-Z-a-z0-9_-]+)");

    private final Jsonb json = JsonbBuilder.create();
    private final String site;
    private final LongSupplier clock;
    private final long lifespan = Duration.ofHours(2).toSeconds();
    private final long tolerance = Duration.ofMinutes(1).toSeconds();

    JwtUtils(String site) {
        this(site, Instant::now);
    }

    JwtUtils(String site, Supplier<Instant> clock) {
        this.site = Objects.requireNonNull(site, "Parameter site for issuer and audience must not be null");
        this.clock = () -> clock.get().toEpochMilli() / 1000;
    }

    String createJsonWebTokenWithHmacSha256(JwtPayload payload, String secret) {
        Objects.requireNonNull(payload, "Parameter payload must not be null");
        Objects.requireNonNull(secret, "Parameter secret must not be null");
        long now = clock.getAsLong();
        long exp = now + lifespan;
        String payloadStr = json.toJson(payload.toBuilder().iss(site).aud(site).iat(now).nbf(now).exp(exp).build());
        String headerStr = json.toJson(new JwtHeader(TYP_JWT, ALG_HS256));
        byte[] signature = createHmacSha256(encodeBase64Url(headerStr) + "." + encodeBase64Url(payloadStr), secret);
        return encodeBase64Url(headerStr) + "." + encodeBase64Url(payloadStr) + "." + encodeBase64Url(signature);
    }

    JwtPayload validateJsonWebTokenWithHmacSha256(String token, String secret) {
        Objects.requireNonNull(token, "Parameter token must not be null");
        Objects.requireNonNull(secret, "Parameter secret must not be null");
        Matcher matcher = TOKEN_PATTERN.matcher(token);
        if (!matcher.matches()) {
            throw new JwtValidationExeption("Token has invalid format");
        }
        String headerStr = decodeBase64toString(matcher.group(1));
        String payloadStr = decodeBase64toString(matcher.group(2));
        byte[] signature = decodeBase64(matcher.group(3));
        byte[] signatureExpected = createHmacSha256(encodeBase64Url(headerStr) + "." + encodeBase64Url(payloadStr), secret);
        if (!Arrays.equals(signature, signatureExpected)) {
            throw new JwtValidationExeption("Token signature mismatch");
        }
        JwtHeader header = json.fromJson(headerStr, JwtHeader.class);
        if (!Objects.equals(header.typ(), TYP_JWT) || !Objects.equals(header.alg(), ALG_HS256)) {
            throw new JwtValidationExeption("Token header unsupported: typ=" + header.typ() + ", alg=" + header.alg());
        }
        JwtPayload payload = json.fromJson(payloadStr, JwtPayload.class);
        if (!Objects.equals(payload.iss(), site)) {
            throw new JwtValidationExeption("Token issuer mismatch: iss=" + payload.iss());
        }
        if (!Objects.equals(payload.aud(), site)) {
            throw new JwtValidationExeption("Token audience mismatch: aud=" + payload.aud());
        }
        long now = clock.getAsLong();
        if(payload.nbf() != null && payload.nbf() - tolerance > now) {
            throw new JwtValidationExeption("Token not yet valid: nbf=" + payload.nbf());
        }
        if(payload.exp() != null && payload.exp() + tolerance < now) {
            throw new JwtValidationExeption("Token has expired: exp=" + payload.exp());
        }
        return payload;
    }

    private byte[] createHmacSha256(String data, String secret) {
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

    private String encodeBase64Url(String data) {
        return encodeBase64Url(data.getBytes(UTF_8));
    }

    private String encodeBase64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private String decodeBase64toString(String data) {
        return new String(decodeBase64(data), UTF_8);
    }

    private byte[] decodeBase64(String data) {
        return Base64.getUrlDecoder().decode(data);
    }
}

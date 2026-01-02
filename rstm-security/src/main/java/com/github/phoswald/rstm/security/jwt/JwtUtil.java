package com.github.phoswald.rstm.security.jwt;

import static com.github.phoswald.rstm.security.Principal.LOCAL_PROVIDER;
import static com.github.phoswald.rstm.security.jwt.JwtHeader.ALG_HS256;
import static com.github.phoswald.rstm.security.jwt.JwtHeader.ALG_RS256;
import static com.github.phoswald.rstm.security.jwt.JwtHeader.TYP_JWT;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.phoswald.rstm.databind.Databinder;

public class JwtUtil {

    private static final Databinder BINDER = new Databinder().pretty(false);
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "([A-Z-a-z0-9_-]+)\\.([A-Z-a-z0-9_-]+)\\.([A-Z-a-z0-9_-]+)");

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final LongSupplier clock;
    private final long clockSkew = Duration.ofMinutes(1).toSeconds();
    private final long lifespan = Duration.ofHours(2).toSeconds();

    public JwtUtil(Supplier<Instant> clock) {
        this.clock = () -> clock.get().toEpochMilli() / 1000;
    }

    public String createTokenWithHmac(JwtPayload payload, String issuer, String secret) {
        Objects.requireNonNull(payload, "Parameter payload must not be null");
        Objects.requireNonNull(issuer, "Parameter issuer must not be null");
        Objects.requireNonNull(secret, "Parameter secret must not be null");
        long now = clock.getAsLong();
        long exp = now + lifespan;
        String payloadStr = BINDER.toJson(payload.toBuilder().iss(issuer).aud(issuer).iat(now).nbf(now).exp(exp).build());
        String headerStr = BINDER.toJson(JwtHeader.builder().typ(TYP_JWT).alg(ALG_HS256).build());
        byte[] signature = createHmacSha256(encodeBase64(headerStr) + "." + encodeBase64(payloadStr), secret);
        return encodeBase64(headerStr) + "." + encodeBase64(payloadStr) + "." + encodeBase64(signature);
    }

    public Optional<JwtValidToken> validateTokenWithHmac(String token, String issuer, String secret) {
        Objects.requireNonNull(token, "Parameter token must not be null");
        Objects.requireNonNull(issuer, "Parameter issuer must not be null");
        Objects.requireNonNull(secret, "Parameter secret must not be null");
        Matcher matcher = TOKEN_PATTERN.matcher(token);
        if (!matcher.matches()) {
            logger.warn("Token has invalid format");
            return Optional.empty();
        }
        String headerStr = decodeBase64toString(matcher.group(1));
        String payloadStr = decodeBase64toString(matcher.group(2));
        byte[] signature = decodeBase64(matcher.group(3));
        JwtHeader header = BINDER.fromJson(headerStr, JwtHeader.class);
        if (!Objects.equals(header.typ(), TYP_JWT) || !Objects.equals(header.alg(), ALG_HS256)) {
            logger.warn("Token header unsupported: typ={}, alg={}", header.typ(), header.alg());
            return Optional.empty();
        }
        byte[] signatureExpected = createHmacSha256(encodeBase64(headerStr) + "." + encodeBase64(payloadStr), secret);
        if (!Arrays.equals(signature, signatureExpected)) {
            logger.warn("Token signature mismatch (HMAC)");
            return Optional.empty();
        }
        JwtPayload payload = BINDER.fromJson(payloadStr, JwtPayload.class);
        if (!Objects.equals(payload.iss(), issuer) || !Objects.equals(payload.aud(), issuer)) {
            logger.warn("Token issuer or audience mismatch: iss={}, aud={}", payload.iss(), payload.aud());
            return Optional.empty();
        }
        long now = clock.getAsLong();
        if (payload.nbf() != null && payload.nbf() - clockSkew > now) {
            logger.warn("Token not yet valid: nbf={}", payload.nbf());
            return Optional.empty();
        }
        if (payload.exp() != null && payload.exp() + clockSkew < now) {
            logger.warn("Token has expired: exp={}", payload.exp());
            return Optional.empty();
        }
        return Optional.of(new JwtValidToken(LOCAL_PROVIDER, token, payload));
    }

    public Optional<JwtValidToken> validateTokenWithRsa(String token, String issuer, String audience, String provider, JwtKeySet keyset) {
        Objects.requireNonNull(token, "Parameter token must not be null");
        Objects.requireNonNull(issuer, "Parameter issuer must not be null");
        Objects.requireNonNull(audience, "Parameter audience must not be null");
        Objects.requireNonNull(keyset, "Parameter keysets must not be null");
        Matcher matcher = TOKEN_PATTERN.matcher(token);
        if (!matcher.matches()) {
            logger.warn("Token has invalid format");
            return Optional.empty();
        }
        String headerStr = decodeBase64toString(matcher.group(1));
        String payloadStr = decodeBase64toString(matcher.group(2));
        byte[] signature = decodeBase64(matcher.group(3));
        JwtHeader header = BINDER.fromJson(headerStr, JwtHeader.class);
        if (/*!Objects.equals(header.typ(), TYP_JWT) || */ !Objects.equals(header.alg(), ALG_RS256)) { // not set by Dex
            logger.warn("Token header unsupported: typ={}, alg={}", header.typ(), header.alg());
            return Optional.empty();
        }
        JwtKey jwtKey = findJwtKey(keyset, header.kid());
        if (jwtKey == null) {
            logger.warn("Key with kid={} not found in JwtKeySet", header.kid());
            return Optional.empty();
        }
        PublicKey publicKey = createPublicKeyRsa(decodeBase64(jwtKey.n()), decodeBase64(jwtKey.e()));
        if (!verifySignatureSha256Rsa(encodeBase64(headerStr) + "." + encodeBase64(payloadStr), signature, publicKey)) {
            logger.warn("Token signature mismatch (RSA)");
            return Optional.empty();
        }
        JwtPayload payload = BINDER.fromJson(payloadStr, JwtPayload.class);
        if (!Objects.equals(payload.iss(), issuer) || !Objects.equals(payload.aud(), audience)) {
            logger.warn("Token issuer or audience mismatch: iss={}, aud={}", payload.iss(), payload.aud());
            return Optional.empty();
        }
        long now = clock.getAsLong();
        if (payload.nbf() != null && payload.nbf() - clockSkew > now) {
            logger.warn("Token not yet valid: nbf={}", payload.nbf());
            return Optional.empty();
        }
        if (payload.exp() != null && payload.exp() + clockSkew < now) {
            logger.warn("Token has expired: exp={}", payload.exp());
            return Optional.empty();
        }
        return Optional.of(new JwtValidToken(provider, token, payload));
    }

    private byte[] createHmacSha256(String data, String secret) {
        try {
            Key key = new SecretKeySpec(secret.getBytes(UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            mac.update(data.getBytes(UTF_8));
            return mac.doFinal();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute HMAC with SHA-256", e);
        }
    }

    private JwtKey findJwtKey(JwtKeySet keyset, String kid) {
        return keyset.keys().stream()
                .filter(k -> Objects.equals(k.use(), "sig"))
                .filter(k -> Objects.equals(k.kty(), "RSA"))
                .filter(k -> Objects.equals(k.kid(), kid))
                // .filter(k -> Objects.equals(k.alg(), ALG_RS256)) // not set by Microsoft
                .findFirst().orElse(null);
    }

    private PublicKey createPublicKeyRsa(byte[] modulus, byte[] exponent) {
        try {
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(new BigInteger(1, modulus), new BigInteger(1, exponent));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to create RSA public key", e);
        }
    }

    private boolean verifySignatureSha256Rsa(String data, byte[] signature, PublicKey publicKey) {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(data.getBytes(UTF_8));
            return sig.verify(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new IllegalStateException("Failed to verify RSA signature", e);
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

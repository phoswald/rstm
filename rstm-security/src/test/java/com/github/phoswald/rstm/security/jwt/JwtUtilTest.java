package com.github.phoswald.rstm.security.jwt;

import static com.github.phoswald.rstm.security.Principal.LOCAL_PROVIDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/*
 * Tokens can be parsed AND created on https://jwt.io/
 */
class JwtUtilTest {

    private static final String HMAC_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJodHRwczovL2V4YW1wbGUuY29tL3RlbmFudDEiLCJleHAiOjE3Mjc5NzU0MjAsImdyb3VwcyI6WyJyb2xlMSIsInJvbGUzIl0sImlhdCI6MTcyNzk2ODIyMCwiaXNzIjoiaHR0cHM6Ly9leGFtcGxlLmNvbS90ZW5hbnQxIiwibmJmIjoxNzI3OTY4MjIwLCJzdWIiOiJ1c2VybmFtZTEifQ.XtC2gjDOLArHg1PP9-8GqR8xlP9AkWZ2-5B0eV4aJnA";
    private static final String HMAC_ISSUER = "https://example.com/tenant1";
    private static final String HMAC_SECRET = "secret1";

    private static final String RSA_TOKEN = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImQ1OTgzNGNlOTc3YmVkNDZkNmE1NGI0NjZjZjdiODk3NzBhOWZiOTIifQ.eyJpc3MiOiJodHRwOi8vMTI3LjAuMC4xOjU1NTYvZGV4Iiwic3ViIjoiQ2cwd0xUTTROUzB5T0RBNE9TMHdFZ1J0YjJOciIsImF1ZCI6InJzdG0tYXBwIiwiZXhwIjoxNzI4ODMzMDQzLCJpYXQiOjE3Mjg3NDY2NDMsImF0X2hhc2giOiJPRmowVHg1MHRTOTVReERZb1hFZnVnIiwiY19oYXNoIjoielNRbEEyWlprQUxIWW9jcklIQ0F3ZyIsImVtYWlsIjoia2lsZ29yZUBraWxnb3JlLnRyb3V0IiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOiJLaWxnb3JlIFRyb3V0In0.py5M1sFUHZVhQJjooI299tdAWBsdLaZebD7j2TpBNCsiWldcrJtV8HRejADrpfuNX-XvqytbLATYbSQ1AMV8J95mfaFvkbrWAUFY2MXF81F2Fzp1xXAdRL-PlwkYsUiWanfOiJE3EZFr-EQURvCng9VW9wO_mqvRpf7NjCDvYa0kk_H5ntGhoH76dSRQH4gY_5-58SfiPfQdEZSzg0khQzyLlD5jJuxTo6HKrh3BkyPYsgRszX1C8sKKuckDPbuPKKLkRNMwA48fuNrFeEhI8RrByrCTPjZtFGFlFiktZijXJ2T7ji6-mieyXfSK8rZSxHzCueauU0hqoAKa6WHSZg";
    private static final String RSA_ISSUER = "http://127.0.0.1:5556/dex";
    private static final String RSA_AUDIENCE = "rstm-app";
    private static final String RSA_PROVIDER = "dexx";
    private static final JwtKeySet RSA_KEYSET = new JwtKeySet(List.of(JwtKey.builder()
            .use("sig")
            .kty("RSA")
            .kid("d59834ce977bed46d6a54b466cf7b89770a9fb92")
            .alg("RS256")
            .n("q3AjsR2c1NwfKpQ80skbunDd2uD7ezNOYW1xIAYg2wVOh-2RR5wQfLQRqyj-iinYjebyj8B2NHNJS3wsx5O8LhAhGRjqeQ2wjkW_M5QPunLNj-Mo8r6iFgIVS8TAdGPxKYbS7uVEU0LIrdBqKL2KyxTtWV7M1sfYIGUI819Y-VhyEZYxW7b31tLHSDNCLGvUHLOqLZ0UZslCuMjQOD02_3BgyYKCwXbNOnFsrlRZ27sJp8I1bykOEIxbg1HGP7Cw2eMY4qllWS1GCVZydyo8JKV5eGT8fOjRzddtnvtPPTGTvMZaeAzsmDXEKZZFiJ9-6QmheX51awd2Je780JR44Q")
            .e("AQAB")
            .build()));

    private final Instant now = Instant.ofEpochSecond(1727968220); // 2024-10-03 15:10:20 UTC
    private final JwtUtil testee = new JwtUtil(() -> now);

    @Test
    void createTokenWithHmac() {
        JwtPayload payload = JwtPayload.of("username1", List.of("role1", "role3"));
        String token = testee.createTokenWithHmac(payload, HMAC_ISSUER, HMAC_SECRET);
        assertEquals(HMAC_TOKEN, token);
    }

    @Test
    void validateTokenWithHmac_validToken_success() {
        Optional<JwtValidToken> token = testee.validateTokenWithHmac(HMAC_TOKEN, HMAC_ISSUER, HMAC_SECRET);
        assertTrue(token.isPresent());
        assertEquals("username1", token.get().payload().determineUser());
        assertEquals(List.of("role1", "role3"), token.get().payload().determineRoles());
        assertEquals(LOCAL_PROVIDER, token.get().provider());
        assertSame(HMAC_TOKEN, token.get().token());
    }

    @Test
    void validateTokenWithHmac_invalidToken_failure() {
        assertFalse(testee.validateTokenWithHmac("bad", HMAC_ISSUER, HMAC_SECRET).isPresent());
    }

    @Test
    void validateTokenWithRsa_validToken_success() {
        Optional<JwtValidToken> token = testee.validateTokenWithRsa(RSA_TOKEN, RSA_ISSUER, RSA_AUDIENCE, RSA_PROVIDER, RSA_KEYSET);
        assertTrue(token.isPresent());
        assertEquals("kilgore@kilgore.trout", token.get().payload().determineUser());
        assertEquals(List.of(), token.get().payload().determineRoles());
        assertEquals(RSA_PROVIDER, token.get().provider());
        assertSame(RSA_TOKEN, token.get().token());
    }

    @Test
    void validateTokenWithRsa_invalidToken_failure() {
        assertFalse(testee.validateTokenWithRsa("bad", RSA_ISSUER, RSA_AUDIENCE, RSA_PROVIDER, RSA_KEYSET).isPresent());
    }
}

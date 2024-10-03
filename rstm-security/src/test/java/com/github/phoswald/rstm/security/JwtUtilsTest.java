package com.github.phoswald.rstm.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

/*
 * Tokens can be parsed AND created on https://jwt.io/
 */
class JwtUtilsTest {

    private final Instant now = Instant.ofEpochSecond(1727968220); // 2024-10-03 15:10:20 UTC
    private final JwtUtils utils = new JwtUtils("https://example.com", () -> now);

    private static final String SECRET = "1234";
    private static final String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJodHRwczovL2V4YW1wbGUuY29tIiwiZXhwIjoxNzI3OTc1NDIwLCJncm91cHMiOlsicm9sZTEiLCJyb2xlMyJdLCJpYXQiOjE3Mjc5NjgyMjAsImlzcyI6Imh0dHBzOi8vZXhhbXBsZS5jb20iLCJuYmYiOjE3Mjc5NjgyMjAsInN1YiI6InVzZXJuYW1lMSJ9.xTT7EFGocWLtHeRSCIoZZMhYN4bM8Op65rATsy_cQeQ"; 
    
    @Test
    void createJsonWebTokenWithHmacSha256() {
        JwtPayload payload = JwtPayload.of("username1", List.of("role1", "role3"));
        String token = utils.createJsonWebTokenWithHmacSha256(payload, SECRET);
        System.out.println(token);
        assertEquals(TOKEN, token);
    }
    
    @Test
    void validateJsonWebTokenWithHmacSha256() {
        JwtPayload payload = utils.validateJsonWebTokenWithHmacSha256(TOKEN, SECRET);
        assertNotNull(payload);
        assertEquals("username1", payload.username());
        assertEquals(List.of("role1", "role3"), payload.roles());
    }
}

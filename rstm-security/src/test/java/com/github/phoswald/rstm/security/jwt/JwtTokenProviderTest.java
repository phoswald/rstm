package com.github.phoswald.rstm.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.phoswald.rstm.security.Principal;

/*
 * Tokens can be parsed AND created on https://jwt.io/
 */
class JwtTokenProviderTest {

    private static final String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJleGFtcGxlLmNvbS90ZW5hbnQxIiwiZXhwIjoxNzI3OTc1NDIwLCJncm91cHMiOlsicm9sZTEiLCJyb2xlMyJdLCJpYXQiOjE3Mjc5NjgyMjAsImlzcyI6ImV4YW1wbGUuY29tL3RlbmFudDEiLCJuYmYiOjE3Mjc5NjgyMjAsInN1YiI6InVzZXJuYW1lMSJ9.BVSUJ49w02zqhAyqLGKW2tinwxCcgDH_zUm0tm0j03U"; 

    private final Instant now = Instant.ofEpochSecond(1727968220); // 2024-10-03 15:10:20 UTC
    private final JwtTokenProvider testee = new JwtTokenProvider("example.com/tenant1", "secret1", () -> now);
    
    @Test
    void createPrincipal() {
        Principal principal = testee.createPrincipal("username1", List.of("role1", "role3"));
        assertEquals("username1", principal.name());
        assertEquals(List.of("role1", "role3"), principal.roles());
        assertEquals(TOKEN, principal.token());
    }

    @Test
    void authenticate_validToken_success() {
        Principal principal = testee.authenticate(TOKEN).get();
        assertEquals("username1", principal.name());
        assertEquals(List.of("role1", "role3"), principal.roles());
    }
    
    @Test
    void authenticate_invalidToken_failure() {
        assertFalse(testee.authenticate("bad").isPresent());
    }
}

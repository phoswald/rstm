package com.github.phoswald.rstm.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.github.phoswald.rstm.security.Principal;

class JwtTokenProviderTest {

    private static final String ISSUER = "https://example.com/tenant1";
    private static final String SECRET = "secret1";
    private static final String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJodHRwczovL2V4YW1wbGUuY29tL3RlbmFudDEiLCJleHAiOjE3Mjc5NzU0MjAsImdyb3VwcyI6WyJyb2xlMSIsInJvbGUzIl0sImlhdCI6MTcyNzk2ODIyMCwiaXNzIjoiaHR0cHM6Ly9leGFtcGxlLmNvbS90ZW5hbnQxIiwibmJmIjoxNzI3OTY4MjIwLCJzdWIiOiJ1c2VybmFtZTEifQ.XtC2gjDOLArHg1PP9-8GqR8xlP9AkWZ2-5B0eV4aJnA"; 

    private final Instant now = Instant.ofEpochSecond(1727968220); // 2024-10-03 15:10:20 UTC
    private final JwtTokenProvider testee = new JwtTokenProvider(ISSUER, SECRET, () -> now);
    
    @Test
    void createPrincipal() {
        Principal principal = testee.createPrincipal("username1", List.of("role1", "role3"));
        assertEquals("username1", principal.name());
        assertEquals(List.of("role1", "role3"), principal.roles());
        assertEquals(TOKEN, principal.token());
    }

    @Test
    void authenticate_validToken_success() {
        Optional<Principal> principal = testee.authenticate(TOKEN);
        assertTrue(principal.isPresent());
        assertEquals("username1", principal.get().name());
        assertEquals(List.of("role1", "role3"), principal.get().roles());
    }
    
    @Test
    void authenticate_invalidToken_failure() {
        assertFalse(testee.authenticate("bad").isPresent());
    }
}

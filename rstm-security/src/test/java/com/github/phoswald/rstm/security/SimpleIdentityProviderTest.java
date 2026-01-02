package com.github.phoswald.rstm.security;

import static com.github.phoswald.rstm.security.Principal.LOCAL_PROVIDER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesRegex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

class SimpleIdentityProviderTest {

    private final IdentityProvider testee = new SimpleIdentityProvider()
            .withUser("username1", "password1", List.of("role1", "role3"))
            .withUser("username2", "password2", List.of("role2"));

    @Test
    void authenticateWithPassword_valid_success() {
        Principal principal = testee.authenticateWithPassword("username1", "password1".toCharArray()).get();
        assertEquals("username1", principal.name());
        assertEquals(List.of("role1", "role3"), principal.roles());
        assertEquals(LOCAL_PROVIDER, principal.provider());
        assertThat(principal.token(), matchesRegex("[0-9a-f]{32}"));
    }

    @Test
    void authenticateWithPassword_invalidPassword_failure() {
        assertFalse(testee.authenticateWithPassword("username1", "bad".toCharArray()).isPresent());
    }

    @Test
    void authenticateWithPassword_invalidUsername_failure() {
        assertFalse(testee.authenticateWithPassword("bad", "bad".toCharArray()).isPresent());
    }

    @Test
    void authenticateWithToken_valid_success() {
        String token = testee.authenticateWithPassword("username1", "password1".toCharArray()).get().token();

        Principal principal = testee.authenticateWithToken(token).get();
        assertEquals("username1", principal.name());
        assertEquals(List.of("role1", "role3"), principal.roles());
        assertEquals(LOCAL_PROVIDER, principal.provider());
        assertThat(principal.token(), matchesRegex("[0-9a-f]{32}"));
    }

    @Test
    void authenticateWithToken_invalid_failure() {
        assertFalse(testee.authenticateWithToken("bad").isPresent());
    }
}

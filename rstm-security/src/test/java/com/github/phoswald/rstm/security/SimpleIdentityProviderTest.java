package com.github.phoswald.rstm.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesRegex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

class SimpleIdentityProviderTest {

    private final IdentityProvider testee = new SimpleIdentityProvider() //
            .add("username1", "password1".toCharArray(), List.of("role1", "role3")) //
            .add("username2", "password2".toCharArray(), List.of("role2"));

    @Test
    void authenticate_valid_success() {
        Principal principal = testee.authenticate("username1", "password1".toCharArray()).get();
        assertEquals("username1", principal.name());
        assertEquals(List.of("role1", "role3"), principal.roles());
        assertThat(principal.token(), matchesRegex("[0-9a-f]{32}"));
    }

    @Test
    void authenticate_invalidPassword_failure() {
        assertFalse(testee.authenticate("username1", "bad".toCharArray()).isPresent());
    }

    @Test
    void authenticate_invalidUsername_failure() {
        assertFalse(testee.authenticate("bad", "bad".toCharArray()).isPresent());
    }

    @Test
    void authenticate_validToken_success() {
        Principal principalPrepared = testee.authenticate("username1", "password1".toCharArray()).get();

        Principal principal = testee.authenticate(principalPrepared.token()).get();
        assertSame(principalPrepared, principal);
    }

    @Test
    void authenticate_invalidToken_failure() {
        assertFalse(testee.authenticate("bad").isPresent());
    }
}

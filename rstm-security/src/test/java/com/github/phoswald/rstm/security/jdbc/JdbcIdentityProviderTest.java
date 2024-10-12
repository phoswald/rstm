package com.github.phoswald.rstm.security.jdbc;

import static com.github.phoswald.rstm.security.Principal.LOCAL_PROVIDER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesRegex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.phoswald.rstm.security.IdentityProvider;
import com.github.phoswald.rstm.security.Principal;
import com.github.phoswald.rstm.security.SimpleTokenProvider;

class JdbcIdentityProviderTest {

    private final IdentityProvider testee = new JdbcIdentityProvider(new SimpleTokenProvider(), this::getConnection);

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
        Principal principalPrepared = testee.authenticateWithPassword("username1", "password1".toCharArray()).get();

        Principal principal = testee.authenticateWithToken(principalPrepared.token()).get();
        assertSame(principalPrepared, principal);
    }

    @Test
    void authenticateWithToken_invalid_failure() {
        assertFalse(testee.authenticateWithToken("bad").isPresent());
    }

    private Connection getConnection() {
        try {
            String url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'src/test/resources/schema.h2.sql'";
            return DriverManager.getConnection(url, "sa", "sa");
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}

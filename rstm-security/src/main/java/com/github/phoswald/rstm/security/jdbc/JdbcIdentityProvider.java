package com.github.phoswald.rstm.security.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.phoswald.rstm.security.IdentityProvider;
import com.github.phoswald.rstm.security.Principal;
import com.github.phoswald.rstm.security.TokenProvider;
import com.github.phoswald.rstm.security.password.PasswordUtility;

/**
 * A local IDP that stores users, hashed passwords and roles in a SQL database.
 */
public class JdbcIdentityProvider implements IdentityProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TokenProvider tokenProvider;
    private final Supplier<Connection> connectionFactory;

    public JdbcIdentityProvider(TokenProvider tokenProvider, Supplier<Connection> connectionFactory) {
        this.tokenProvider = Objects.requireNonNull(tokenProvider);
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
    }

    @Override
    public Optional<Principal> authenticateWithPassword(String username, char[] password) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
        JdbcUser userEntity = selectUser(username);
        if (userEntity != null && PasswordUtility.verifyPassword(password, userEntity.hashedPassword())) {
            logger.info("Login successful for username={}", username);
            return Optional.of(tokenProvider.createPrincipal(userEntity.username(), userEntity.rolesAsList()));
        } else {
            logger.warn("Login failed for username={}", username);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Principal> authenticateWithToken(String token) {
        return tokenProvider.authenticateWithToken(token);
    }

    private JdbcUser selectUser(String username) {
        try (Connection connection = connectionFactory.get()) {
            PreparedStatement statement = connection.prepareStatement("""
                    SELECT username_, password_, roles_
                    FROM user_
                    WHERE username_ = ?
                    """);
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return JdbcUser.builder()
                            .username(resultSet.getString("username_"))
                            .hashedPassword(resultSet.getString("password_"))
                            .roles(resultSet.getString("roles_"))
                            .build();
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}

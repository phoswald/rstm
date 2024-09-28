package com.github.phoswald.rstm.security;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.BCrypt.Version;

/**
 * See https://github.com/patrickfav/bcrypt
 */
public class JdbcIdentityProvider extends IdentityProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Supplier<Connection> connectionFactory;

    public JdbcIdentityProvider(Supplier<Connection> connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Optional<Principal> authenticate(String username, char[] password) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
        JdbcUser userEntity = selectUser(username);
        if(userEntity != null && checkPassword(password, userEntity.hashedPassword())) {
            logger.info("Login successful for username={}", username);
            return Optional.of(createPrincipal(userEntity.username(), userEntity.rolesAsList()));
        } else {
            logger.warn("Login failed for username={}", username);
            return Optional.empty();
        }
    }
    
    private JdbcUser selectUser(String username) {
        try(Connection connection = connectionFactory.get()) {
            PreparedStatement statement = connection.prepareStatement("""
                    SELECT username_, password_, roles_
                    FROM user_
                    WHERE username_ = ?
                    """);
            statement.setString(1, username);
            try(ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return JdbcUser.builder() //
                            .username(resultSet.getString("username_")) //
                            .hashedPassword(resultSet.getString("password_")) //
                            .roles(resultSet.getString("roles_")) //
                            .build();
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private boolean checkPassword(char[] enteredPassword, String hashedPassword) {
        return BCrypt.verifyer().verify(enteredPassword, hashedPassword).verified;
    }
    
    public static String hashPassword(char[] password) {
        return BCrypt.with(Version.VERSION_2A).hashToString(10, password);
    }
}

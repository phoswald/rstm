package com.github.phoswald.rstm.security.jdbc;

import static java.util.function.Predicate.not;

import java.util.List;

import com.github.phoswald.record.builder.RecordBuilder;

@RecordBuilder
record JdbcUser(String username, String hashedPassword, String roles) {

    List<String> rolesAsList() {
        return List.of(roles.split(",")).stream().map(String::trim).filter(not(String::isBlank)).toList();
    }

    static JdbcUserBuilder builder() {
        return new JdbcUserBuilder();
    }
}

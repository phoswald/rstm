package com.github.phoswald.rstm.config;

import java.util.Optional;

public class ConfigProvider {

    public Optional<String> getConfigProperty(String name) {
        String value = System.getProperty(name);
        if (value == null) {
            value = System.getenv().get(name.replace('.', '_').toUpperCase());
        }
        if (value != null) {
            return Optional.of(value);
        } else {
            return Optional.empty();
        }
    }
}

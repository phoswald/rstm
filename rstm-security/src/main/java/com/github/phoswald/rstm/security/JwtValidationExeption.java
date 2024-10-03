package com.github.phoswald.rstm.security;

public class JwtValidationExeption extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public JwtValidationExeption(String message) {
        super(message);
    }
}

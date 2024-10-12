package com.github.phoswald.rstm.security.jwt;

import java.util.List;

/**
 * Response from JWKS endpoint
 */
public record JwtKeySet(List<JwtKey> keys) { }

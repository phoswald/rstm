package com.github.phoswald.rstm.security.jwt;

public record JwtValidToken(String provider, String token, JwtPayload payload) { }

package com.github.phoswald.rstm.http.health;

public record HealthCheck(int id, String name, HealthCheckFunction function) { }

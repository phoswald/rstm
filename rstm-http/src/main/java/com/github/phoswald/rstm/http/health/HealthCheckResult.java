package com.github.phoswald.rstm.http.health;

public record HealthCheckResult(
        String name,
        HealthCheckStatus status
) {

    public static HealthCheckResult create(String name, boolean up) {
        return new HealthCheckResult(name, up ? HealthCheckStatus.UP : HealthCheckStatus.DOWN);
    }
}

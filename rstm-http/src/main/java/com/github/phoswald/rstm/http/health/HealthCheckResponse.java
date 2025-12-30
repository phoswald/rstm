package com.github.phoswald.rstm.http.health;

import java.util.List;

public record HealthCheckResponse(
        HealthCheckStatus status,
        List<HealthCheckResult> checks
) {

    public static HealthCheckResponse create(boolean up, List<HealthCheckResult> checks) {
        boolean allUp = up && checks.stream().allMatch(check -> check.status() == HealthCheckStatus.UP);
        return new HealthCheckResponse(allUp ? HealthCheckStatus.UP : HealthCheckStatus.DOWN, checks);
    }

    public int httpStatus() {
        return status == HealthCheckStatus.UP ? 200 /* OK */ : 503 /* Service Unavailable */;
    }
}

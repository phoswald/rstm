package com.github.phoswald.rstm.http.health;

import static com.github.phoswald.rstm.http.codec.json.JsonCodec.json;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.get;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.route;

import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.phoswald.rstm.http.HttpResponse;
import com.github.phoswald.rstm.http.server.HttpFilter;

public class HealthCheckRegistry {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final SortedMap<Integer, HealthCheck> checks = new ConcurrentSkipListMap<>();

    public HealthCheck registerCheck(String name, HealthCheckFunction checkFunction) {
        var check = new HealthCheck(nextId.getAndIncrement(), name, checkFunction);
        checks.put(check.id(), check);
        return check;
    }

    public HttpFilter createRoute() {
        return route("/health", get(_ -> checkHealth()));
    }

    private HttpResponse checkHealth() {
        var results = checks.values().stream()
                .map(this::checkHealth)
                .toList();
        var response = HealthCheckResponse.create(true, results);
        return HttpResponse.body(response.httpStatus(), json(), response);
    }

    private HealthCheckResult checkHealth(HealthCheck check) {
        try {
            return HealthCheckResult.create(check.name(), check.function().invoke());
        } catch (Exception e) {
            logger.warn("Exception while checking '{}': {}", check.name(), e.toString());
            return HealthCheckResult.create(check.name(), false);
        }
    }
}

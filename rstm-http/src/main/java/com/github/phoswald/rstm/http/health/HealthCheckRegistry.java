package com.github.phoswald.rstm.http.health;

import static com.github.phoswald.rstm.http.codec.json.JsonCodec.json;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.get;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.route;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.phoswald.rstm.http.HttpResponse;
import com.github.phoswald.rstm.http.server.HttpFilter;

public class HealthCheckRegistry {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final List<Registration> checks = new ArrayList<>();

    public void registerCheck(String name, HealthCheckFunction check) {
        checks.add(new Registration(name, check));
    }

    public HttpFilter createRoute() {
        return route("/health", get(_ -> checkHealth()));
    }

    private HttpResponse checkHealth() {
        var results = checks.stream()
                .map(this::checkHealth)
                .toList();
        var response = HealthCheckResponse.create(true, results);
        return HttpResponse.body(response.httpStatus(), json(), response);
    }

    private HealthCheckResult checkHealth(Registration registration) {
        try {
            return HealthCheckResult.create(registration.name(), registration.function().invoke());
        } catch(Exception e) {
            logger.warn("Exception while checking '{}': {}", registration.name(), e.toString());
            return HealthCheckResult.create(registration.name(), false);
        }
    }

    private record Registration(String name, HealthCheckFunction function) { }
}

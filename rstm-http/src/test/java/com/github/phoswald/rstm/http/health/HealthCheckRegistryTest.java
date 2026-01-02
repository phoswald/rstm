package com.github.phoswald.rstm.http.health;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.phoswald.rstm.http.server.HttpServer;
import com.github.phoswald.rstm.http.server.HttpServerConfig;

class HealthCheckRegistryTest {

    private final HealthCheckRegistry testee = new HealthCheckRegistry();

    private final HttpServerConfig config = HttpServerConfig.builder()
            .httpPort(8080)
            .filter(testee.createRoute())
            .build();

    private final HttpServer server = new HttpServer(config);

    @AfterEach
    void cleanup() {
        server.close();
    }

    @Test
    void getHealth_noChecks_up() {
        when().
                get("/health").
        then().
                statusCode(200).
                contentType("application/json").
                body("status", equalTo("UP"));
    }

    @Test
    void getHealth_checkSuccessful_up() {
        testee.registerCheck(null, () -> true);
        when().
            get("/health").
        then().
            statusCode(200).
            contentType("application/json").
            body("status", equalTo("UP")).
            body("checks[0].name", nullValue()).
            body("checks[0].status", equalTo("UP"));
    }

    @Test
    void getHealth_checkFails_down() {
        testee.registerCheck("sample", () -> false);
        when().
                get("/health").
        then().
                statusCode(503).
                contentType("application/json").
                body("status", equalTo("DOWN")).
                body("checks[0].name", equalTo("sample")).
                body("checks[0].status", equalTo("DOWN"));
    }

    @Test
    void getHealth_checkThrows_down() {
        testee.registerCheck(null, () -> {
            throw new RuntimeException("BOOM!");
        });
        when().
                get("/health").
        then().
                statusCode(503).
                contentType("application/json").
                body("status", equalTo("DOWN")).
                body("checks[0].status", equalTo("DOWN"));
    }
}

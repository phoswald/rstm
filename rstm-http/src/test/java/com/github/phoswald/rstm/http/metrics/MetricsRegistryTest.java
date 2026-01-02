package com.github.phoswald.rstm.http.metrics;

import static io.restassured.RestAssured.when;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.phoswald.rstm.http.server.HttpServer;
import com.github.phoswald.rstm.http.server.HttpServerConfig;

class MetricsRegistryTest {

    private final MetricsRegistry testee = new MetricsRegistry();

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
    void getMetrics_empty_success() {
        var label1 = new MetricLabel("sample_label", "sample_value");
        var label2 = new MetricLabel("sample_label2", "sample_value2");
        testee.registerGauge("sample_gauge", () -> 42.5, label1);
        testee.registerCounter("sample_counter", label1, label2).value().addAndGet(3);

        when()
                .get("/metrics")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(equalTo("""
                        sample_counter{sample_label="sample_value",sample_label2="sample_value2"} 3
                        sample_gauge{sample_label="sample_value"} 42.5
                        """));
    }

    @Test
    void register_invalid_exception() {
        // null or invalid names or values
        assertThrows(IllegalArgumentException.class, () -> testee.registerCounter(null));
        assertThrows(IllegalArgumentException.class, () -> testee.registerCounter("bad-name"));
        assertThrows(IllegalArgumentException.class, () -> testee.registerCounter("name", new MetricLabel(null, "value")));
        assertThrows(IllegalArgumentException.class, () -> testee.registerCounter("name", new MetricLabel("bad-name", "value")));
        assertThrows(IllegalArgumentException.class, () -> testee.registerCounter("name", new MetricLabel("name", null)));

        // duplicate labels
        assertThrows(IllegalArgumentException.class, () -> testee.registerCounter("name",
                new MetricLabel("name", "value"), new MetricLabel("name", "value")));

        // duplicates instances
        testee.registerCounter("name");
        testee.registerCounter("name", new MetricLabel("name", "value1"));
        testee.registerCounter("name", new MetricLabel("name", "value2"));
        assertThrows(IllegalArgumentException.class, () -> testee.registerCounter("name"));
        assertThrows(IllegalArgumentException.class, () -> testee.registerCounter("name", new MetricLabel("name", "value1")));

        // verify all successful registrations are rendered
        assertThat(testee.collectMetrics(), equalTo("""
                name 0
                name{name="value1"} 0
                name{name="value2"} 0
                """));
    }

    @Test
    void collectMetrics_exception_caught() {
        testee.registerGauge("name1", () -> 10);
        testee.registerGauge("name2", () -> {
            throw new IllegalStateException();
        });
        testee.registerGauge("name3", () -> 30);

        // verify all successful values are rendered
        assertThat(testee.collectMetrics(), equalTo("""
                name1 10
                name3 30
                """));
    }
}

package com.github.phoswald.rstm.http.server;

import static com.github.phoswald.rstm.http.server.HttpServerConfig.resources;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.route;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HttpServerWithRootResourcesTest {

    private static final HttpServerConfig config = HttpServerConfig.builder()
            .httpPort(8080)
            .filter(route("/", resources("/html/")))
            .build();

    private static final HttpServer testee = new HttpServer(config);

    @AfterAll
    static void cleanup() {
        testee.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/index.html",
            "/",
            "/subdir/index.html",
            "/subdir/"
    })
    void get_resourceExistingHtml_success(String path) {
        when()
                .get(path)
                .then()
                .statusCode(200)
                .contentType("text/html")
                .body(startsWith("<!doctype html>"))
                .body(containsString("<title>Sample Page</title>"))
                .body(containsString("<h1>Sample Page</h1>"));
    }

    @Test
    void get_resourceExistingIco_success() {
        when()
                .get("/favicon.ico")
                .then()
                .statusCode(200)
                .contentType("image/x-icon")
                .header("content-length", "1406");
    }

    @Test
    void get_resourceNotExisting_notFound() {
        when()
                .get("/missing.html")
                .then()
                .statusCode(404);
    }

    @Test
    void get_resourceInvalidPath_badRequest() {
        when()
                .get("/../simplelogger.properties")
                .then()
                .statusCode(400);
    }
}

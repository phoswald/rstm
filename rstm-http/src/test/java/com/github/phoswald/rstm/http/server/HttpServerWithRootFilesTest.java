package com.github.phoswald.rstm.http.server;

import static com.github.phoswald.rstm.http.server.HttpServerConfig.filesystem;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.route;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HttpServerWithRootFilesTest {

    private static final HttpServerConfig config = HttpServerConfig.builder() //
            .httpPort(8080) //
            .filter(route("/", filesystem(Paths.get("src/test/resources/html/")))) //
            .build();

    private static final HttpServer testee = new HttpServer(config);

    @AfterAll
    static void cleanup() {
        testee.close();
    }

    @ParameterizedTest
    @ValueSource(strings = { //
            "/index.html", //
            "/", //
            "/subdir/index.html", //
            "/subdir/" //
    })
    void get_fileExistingHtml_success(String path) {
        when().
            get(path).
        then().
            statusCode(200).
            contentType("text/html").
            body(
                startsWith("<!doctype html>"),
                containsString("<title>Sample Page</title>"),
                containsString("<h1>Sample Page</h1>"));
    }

    @Test
    void get_fileExistingIco_success() {
        when().
            get("/favicon.ico").
        then().
            statusCode(200).
            contentType("image/x-icon").
            header("content-length", "1406");
    }

    @Test
    void get_fileNotExisting_notFound() {
        when().
            get("/missing.html").
        then().
            statusCode(404);
    }

    @Test
    void get_fileInvalidPath_badRequest() {
        when().
            get("/../simplelogger.properties").
        then().
            statusCode(400);
    }
}

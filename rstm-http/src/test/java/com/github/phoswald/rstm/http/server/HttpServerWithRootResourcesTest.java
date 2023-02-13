package com.github.phoswald.rstm.http.server;

import static com.github.phoswald.rstm.http.server.HttpServerConfig.combine;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.resources;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.route;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HttpServerWithRootResourcesTest {

    private final HttpServerConfig config = HttpServerConfig.builder() //
            .httpPort(8080) //
            .filter(combine( //
                    route("/", //
                            resources("/html/")) //
            )) //
            .build();

    private final HttpServer testee = new HttpServer(config);

    @AfterEach
    void cleanup() {
        testee.close();
    }

    @Test
    void get_resourceExistingHtml_success() {
        when().
            get("/index.html").
        then().
            statusCode(200).
            contentType("text/html").
            body(
                startsWith("<!doctype html>"),
                containsString("<title>Sample Page</title>"),
                containsString("<h1>Sample Page</h1>"));
    }

    @Test
    void get_resourceExistingHtml2_success() {
        when().
            get("/").
        then().
            statusCode(200).
            contentType("text/html").
            body(
                startsWith("<!doctype html>"),
                containsString("<title>Sample Page</title>"),
                containsString("<h1>Sample Page</h1>"));
    }

    @Test
    void get_resourceExistingHtml3_success() {
        when().
            get("/subdir/").
        then().
            statusCode(200).
            contentType("text/html").
            body(
                startsWith("<!doctype html>"),
                containsString("<title>Sample Page</title>"),
                containsString("<h1>Sample Page</h1>"));
    }

    @Test
    void get_resourceExistingHtml4_success() {
        when().
            get("/subdir/index.html").
        then().
            statusCode(200).
            contentType("text/html").
            body(
                startsWith("<!doctype html>"),
                containsString("<title>Sample Page</title>"),
                containsString("<h1>Sample Page</h1>"));
    }

    @Test
    void get_resourceExistingIco_success() {
        when().
            get("/favicon.ico").
        then().
            statusCode(200).
            contentType("image/x-icon").
            header("content-length", "1406");
    }

    @Test
    void get_resourceNotExisting_notFound() {
        when().
            get("/missing.html").
        then().
            statusCode(404);
    }

    @Test
    void get_resourceEscapedPath_badRequest() {
        when().
            get("/../simplelogger.properties").
        then().
            statusCode(400);
    }
}

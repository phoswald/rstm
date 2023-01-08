package com.github.phoswald.rstm.http.server;

import static com.github.phoswald.rstm.http.server.HttpServerConfig.all;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.filesystem;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.get;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.resources;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.route;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.phoswald.rstm.http.HttpResponse;

class HttpServerTest {

    private final HttpServerConfig config = HttpServerConfig.builder() //
            .httpPort(8080) //
            .handler(all( //
                    route("/static/resources/", //
                            resources("/html/")), //
                    route("/static/files/", //
                            filesystem(Paths.get("src/test/resources/html/"))), //
                    route("/dynamic/sample", //
                            get(request -> HttpResponse.builder() //
                                    .contentType("text/plain") //
                                    .body("Sample String".getBytes(StandardCharsets.UTF_8)) //
                                    .build())))) //
            .build();
    private final HttpServer testee = new HttpServer(config);

    @AfterEach
    void cleanup() {
        testee.close();
    }

    @Test
    void get_noRoute_notFound() {
        when().
            get("/undefined").
        then().
            statusCode(404);
    }

    @Test
    void get_resourceExistingHtml_success() {
        when().
            get("/static/resources/index.html").
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
            get("/static/resources/favicon.ico").
        then().
            statusCode(200).
            contentType("image/x-icon").
            header("content-length", "1406");
    }

    @Test
    void get_resourceNotExisting_notFound() {
        when().
            get("/static/resources/missing.html").
        then().
            statusCode(404);
    }

    @Test
    void get_resourceEscapedPath_badRequest() {
        when().
            get("/static/resources/../simplelogger.properties").
        then().
            statusCode(400);
    }

    @Test
    void get_fileExistingHtml_success() {
        when().
            get("/static/files/index.html").
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
            get("/static/files/favicon.ico").
        then().
            statusCode(200).
            contentType("image/x-icon").
            header("content-length", "1406");
    }

    @Test
    void get_fileNotExisting_notFound() {
        when().
            get("/static/files/missing.html").
        then().
            statusCode(404);
    }

    @Test
    void get_fileEscapedPath_badRequest() {
        when().
            get("/static/files/../simplelogger.properties").
        then().
            statusCode(400);
    }

    @Test
    void get_dynamicExisting_success() {
        when().
            get("/dynamic/sample").
        then().
            statusCode(200).
            contentType("text/plain").
            body(equalTo("Sample String"));
    }
}

package com.github.phoswald.rstm.http.server;

import static com.github.phoswald.rstm.http.server.HttpServerConfig.combine;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.delete;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.filesystem;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.get;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.post;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.put;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.resources;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.route;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.phoswald.rstm.http.HttpResponse;

class HttpServerTest {

    private final HttpServerConfig config = HttpServerConfig.builder() //
            .httpPort(8080) //
            .filter(combine( //
                    route("static/resources/", //
                            resources("/html/")), //
                    route("static/files/", //
                            filesystem(Paths.get("src/test/resources/html/"))), //
                    route("dynamic/sample", //
                            get(request -> HttpResponse.text(200, "Response for GET")), //
                            post(request -> HttpResponse.text(200, "Response for POST of " + request.text())), //
                            put(request -> HttpResponse.text(200, "Response for PUT of " + request.text())), //
                            delete(request -> HttpResponse.text(200, "Response for DELETE"))), //
                    route("dynamic/param/{name}", //
                            get(request -> HttpResponse.text(200, "Response for GET with p1=" + request.pathParam("name").orElse(null)))), //
                    route("dynamic/query", //
                            get(request -> HttpResponse.text(200, "Response for GET with q1=" + request.queryParam("q1").orElse(null) + " and q2=" + request.queryParam("q2").orElse(null)))), //
                    route("dynamic/form", //
                            post(request -> HttpResponse.text(200, "Response for POST with f1=" + request.formParam("f1").orElse(null) + " and f2=" + request.formParam("f2").orElse(null))), //
                            put(request -> HttpResponse.text(200, "Response for PUT with f1=" + request.formParam("f1").orElse(null) + " and f2=" + request.formParam("f2").orElse(null)))), //
                    route("dynamic/redirect", get(request -> HttpResponse.redirect(302, "/dynamic/other"))) //
            )) //
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
            body(equalTo("Response for GET"));
    }

    @Test
    void post_dynamicExisting_success() {
        given().
            contentType("text/plain").
            body("Sample Request").
        when().
            post("/dynamic/sample").
        then().
            statusCode(200).
            contentType("text/plain").
            body(equalTo("Response for POST of Sample Request"));
    }

    @Test
    void put_dynamicExisting_success() {
        given().
            contentType("text/plain").
            body("Sample Request").
        when().
            put("/dynamic/sample").
        then().
            statusCode(200).
            contentType("text/plain").
            body(equalTo("Response for PUT of Sample Request"));
    }

    @Test
    void delete_dynamicExisting_success() {
        when().
            delete("/dynamic/sample").
        then().
            statusCode(200).
            contentType("text/plain").
            body(equalTo("Response for DELETE"));
    }

    @Test
    void get_dynamicPathParam_success() {
        when().
            get("/dynamic/param/1234").
        then().
            statusCode(200).
            body(equalTo("Response for GET with p1=1234"));
    }

    @Test
    void get_dynamicQueryParam_success() {
        given().
            queryParam("q1", "search").
            queryParam("q2", "more text").
        when().
            get("/dynamic/query").
        then().
            statusCode(200).
            body(equalTo("Response for GET with q1=search and q2=more text"));
    }

    @Test
    void post_dynamicFormParam_success() {
        given().
            formParam("f1", "search").
            formParam("f2", "more text").
        when().
            post("/dynamic/form").
        then().
            statusCode(200).
            body(equalTo("Response for POST with f1=search and f2=more text"));
    }

    @Test
    void put_dynamicFormParam_success() {
        given().
            formParam("f1", "search").
            formParam("f2", "more text").
        when().
            put("/dynamic/form").
        then().
            statusCode(200).
            body(equalTo("Response for PUT with f1=search and f2=more text"));
    }

    @Test
    void get_dynamicRedirect_success() {
        given().
            redirects().follow(false).
        when().
            get("/dynamic/redirect").
        then().
            statusCode(302).
            header("location", "/dynamic/other");
    }
}

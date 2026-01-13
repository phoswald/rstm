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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.phoswald.rstm.http.HttpResponse;

class HttpServerTest {

    // Basic Latin characters (0x20..0x7F, in order): ! " # $ % & ' ( ) * + , - . / : ; < = > ? @ [ \ ] ^ _ ` { | } ~
    private static final String ASCII = "! \" # $ % & ' ( ) * + , - . / : ; < = > ? @ [ \\ ] ^ _ ` { | } ~ \t \r \n";
    private static final String UNICODE = "€ äöü αβγδ กขฃ";

    private static final HttpServerConfig config = HttpServerConfig.builder()
            .httpPort(8080)
            .filter(combine(
                    route("/static/resources/",
                            resources("/html/")),
                    route("/static/files/",
                            filesystem(Paths.get("src/test/resources/html/"))),
                    route("/dynamic",
                            get(_ -> HttpResponse.text(200, "Response for GET")),
                            post(request -> HttpResponse.text(200, "Response for POST of " + request.text())),
                            put(request -> HttpResponse.text(200, "Response for PUT of " + request.text())),
                            delete(_ -> HttpResponse.text(200, "Response for DELETE"))),
                    route("/dynamic/param/{name}",
                            get(request -> HttpResponse.text(200, "Response for GET with name=" + request.pathParam("name").orElse(null)))),
                    route("/dynamic/query",
                            get(request -> HttpResponse.text(200, "Response for GET with q1=" + request.queryParam("q1").orElse(null) + " and q2=" + request.queryParam("q2").orElse(null) + " and q3=" + request.queryParam("q3").orElse(null)))),
                    route("/dynamic/form",
                            post(request -> HttpResponse.text(200, "Response for POST with f1=" + request.formParam("f1").orElse(null) + " and f2=" + request.formParam("f2").orElse(null) + " and f3=" + request.formParam("f3").orElse(null))),
                            put(request -> HttpResponse.text(200, "Response for PUT with f1=" + request.formParam("f1").orElse(null) + " and f2=" + request.formParam("f2").orElse(null) + " and f3=" + request.formParam("f3").orElse(null)))),
                    route("/dynamic/empty",
                            get(_ -> HttpResponse.empty(204))),
                    route("/dynamic/text",
                            get(_ -> HttpResponse.text(200, "Response for GET"))),
                    route("/dynamic/html",
                            get(_ -> HttpResponse.html(200, "<!doctype html><html><head><title>T</title></head><body>B</body></html>"))),
                    route("/dynamic/redirecting",
                            get(_ -> HttpResponse.redirect(302, "/dynamic/other"))),
                    route("/dynamic/notexisting",
                            get(_-> HttpResponse.empty(404))),
                    route("/dynamic/failing",
                            get(_-> { throw new IllegalStateException(""); }))
            ))
            .build();

    private static final HttpServer testee = new HttpServer(config);

    @AfterAll
    static void cleanup() {
        testee.close();
    }

    @Test
    void get_noRoute_notFound() {
        when()
                .get("/undefined")
                .then()
                .statusCode(404);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/static/resources/index.html",
            "/static/resources/",
            "/static/resources/subdir/index.html",
            "/static/resources/subdir/"
    })
    void get_resourceHtml_success(String path) {
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
    void get_resourceIco_success() {
        when()
                .get("/static/resources/favicon.ico")
                .then()
                .statusCode(200)
                .contentType("image/x-icon")
                .header("content-length", "1406");
    }

    @Test
    void get_resourceNotExisting_notFound() {
        when()
                .get("/static/resources/missing.html")
                .then()
                .statusCode(404);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/static/resources/../simplelogger.properties",
            "/static/resources/../",
            "/static/resources/.."
    })
    void get_resourceInvalidPath_badRequest(String path) {
        when()
                .get(path)
                .then()
                .statusCode(400);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/static/files/index.html",
            "/static/files/",
            "/static/files/subdir/index.html",
            "/static/files/subdir/"
    })
    void get_fileHtml_success(String path) {
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
    void get_fileIco_success() {
        when()
                .get("/static/files/favicon.ico")
                .then()
                .statusCode(200)
                .contentType("image/x-icon")
                .header("content-length", "1406");
    }

    @Test
    void get_fileNotExisting_notFound() {
        when()
                .get("/static/files/missing.html")
                .then()
                .statusCode(404);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/static/files/../simplelogger.properties",
            "/static/files/../",
            "/static/files/..",
    })
    void get_fileInvalidPath_badRequest(String path) {
        when()
                .get(path)
                .then()
                .statusCode(400);
    }

    @Test
    void get_dynamic_success() {
        when()
                .get("/dynamic")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(equalTo("Response for GET"));
    }

    @Test
    void post_dynamic_success() {
        given()
                .contentType("text/plain")
                .body("Sample Request")
                .when()
                .post("/dynamic")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(equalTo("Response for POST of Sample Request"));
    }

    @Test
    void put_dynamic_success() {
        given()
                .contentType("text/plain")
                .body("Sample Request")
                .when()
                .put("/dynamic")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(equalTo("Response for PUT of Sample Request"));
    }

    @Test
    void delete_dynamic_success() {
        when()
                .delete("/dynamic")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(equalTo("Response for DELETE"));
    }

    @Test
    void get_dynamicPathParam_success() {
        when()
                .get("/dynamic/param/1234")
                .then()
                .statusCode(200)
                .body(equalTo("Response for GET with name=1234"));
    }

    @Test
    void get_dynamicQueryParam_success() {
        given()
                .queryParam("q1", "search")
                .queryParam("q2", ASCII)
                .queryParam("q3", UNICODE)
                .when()
                .get("/dynamic/query")
                .then()
                .statusCode(200)
                .body(equalTo("Response for GET with q1=search and q2=" + ASCII + " and q3=" + UNICODE));
    }

    @Test
    void post_dynamicFormParam_success() {
        given()
                .contentType("application/x-www-form-urlencoded; charset=utf-8")
                .formParam("f1", "search")
                .formParam("f2", ASCII)
                .formParam("f3", UNICODE)
                .when()
                .post("/dynamic/form")
                .then()
                .statusCode(200)
                .body(equalTo("Response for POST with f1=search and f2=" + ASCII + " and f3=" + UNICODE));
    }

    @Test
    void put_dynamicFormParam_success() {
        given()
                .contentType("application/x-www-form-urlencoded; charset=utf-8")
                .formParam("f1", "search")
                .formParam("f2", ASCII)
                .formParam("f3", UNICODE)
                .when()
                .put("/dynamic/form")
                .then()
                .statusCode(200)
                .body(equalTo("Response for PUT with f1=search and f2=" + ASCII + " and f3=" + UNICODE));
    }

    @Test
    void get_dynamicEmpty_success() {
        when()
                .get("/dynamic/empty")
                .then()
                .statusCode(204);
    }

    @Test
    void get_dynamicText_success() {
        when()
                .get("/dynamic/text")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(equalTo("Response for GET"));
    }

    @Test
    void get_dynamicHtml_success() {
        when()
                .get("/dynamic/html")
                .then()
                .statusCode(200)
                .contentType("text/html")
                .body("html.head.title", equalTo("T"))
                .body("html.body", equalTo("B"));
    }

    @Test
    void get_dynamicRedirecting_statusRedirect() {
        given()
                .redirects().follow(false)
                .when()
                .get("/dynamic/redirecting")
                .then()
                .statusCode(302)
                .header("location", "/dynamic/other");
    }

    @Test
    void get_dynamicNotExisting_statusNotFound() {
        given()
                .redirects().follow(false)
                .when()
                .get("/dynamic/notexisting")
                .then()
                .statusCode(404);
    }

    @Test
    void get_dynamicFailing_statusError() {
        when()
                .get("/dynamic/failing")
                .then()
                .statusCode(500);
    }

    @Test
    void createMetadata_valid_success() {
        List<RouteMetadata> routes = config.filter().createMetadata();
        assertThat(routes.size(), Matchers.greaterThanOrEqualTo(14));
        assertTrue(routes.stream().allMatch(r -> r.route().startsWith("/dynamic")));
        assertTrue(routes.stream().allMatch(r -> r.method() != null));
        assertTrue(routes.stream().anyMatch(r -> r.pathParams().contains("name")));
    }
}

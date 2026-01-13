package com.github.phoswald.rstm.http.server;

import static com.github.phoswald.rstm.http.server.HttpServerConfig.combine;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.getHtml;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.postHtml;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.route;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class HttpServerHtmlTest {

    private static final HttpServerConfig config = HttpServerConfig.builder()
            .httpPort(8080)
            .filter(combine(
                    route("/dynamic",
                            getHtml(_ -> "<!doctype html><html><head><title>T</title></head><body>B</body></html>"),
                            postHtml(_ -> "<!doctype html><html><head><title>T</title></head><body>B</body></html>")),
                    route("/dynamic/param/{name}",
                            getHtml(SampleParams.class, params -> "<!doctype html><html><head><title>T</title></head><body>" + params.name() + "</body></html>")),
                    route("/dynamic/query",
                            getHtml(SampleParams.class, params -> "<!doctype html><html><head><title>T</title></head><body>q1=" + params.q1() + " and q2=" + params.q2() + "</body></html>")),
                    route("/dynamic/form",
                            postHtml(SampleParams.class, params -> "<!doctype html><html><head><title>T</title></head><body>f1=" + params.f1() + " and f2=" + params.f2() + "</body></html>")),
                    route("/dynamic/empty",
                            getHtml(() -> "")),
                    route("/dynamic/redirecting",
                            getHtml(() -> "redirect=/dynamic/other")),
                    route("/dynamic/notexisting",
                            getHtml(() -> null)),
                    route("/dynamic/failing",
                            getHtml(()-> { throw new IllegalStateException(""); }))
            ))
            .build();

    private static final HttpServer testee = new HttpServer(config);

    @AfterAll
    static void cleanup() {
        testee.close();
    }

    @Test
    void get_dynamic_success() {
        when()
                .get("/dynamic")
                .then()
                .statusCode(200)
                .contentType("text/html")
                .body("html.head.title", equalTo("T"))
                .body("html.body", equalTo("B"));
    }

    @Test
    void post_dynamic_success() {
        given()
                .contentType("application/x-www-form-urlencoded; charset=utf-8")
                .when()
                .post("/dynamic")
                .then()
                .statusCode(200)
                .contentType("text/html")
                .body("html.head.title", equalTo("T"))
                .body("html.body", equalTo("B"));
    }

    @Test
    void get_dynamicPathParam_success() {
        when()
                .get("/dynamic/param/1234")
                .then()
                .statusCode(200)
                .contentType("text/html")
                .body("html.head.title", equalTo("T"))
                .body("html.body", equalTo("1234"));
    }

    @Test
    void get_dynamicQueryParam_success() {
        given()
                .queryParam("q1", "search")
                .queryParam("q2", "query")
                .when()
                .get("/dynamic/query")
                .then()
                .statusCode(200)
                .contentType("text/html")
                .body("html.head.title", equalTo("T"))
                .body("html.body", equalTo("q1=search and q2=query"));
    }

    @Test
    void post_dynamicFromParam_success() {
        given()
                .contentType("application/x-www-form-urlencoded; charset=utf-8")
                .formParam("f1", "search")
                .formParam("f2", "query")
                .when()
                .post("/dynamic/form")
                .then()
                .statusCode(200)
                .contentType("text/html")
                .body("html.head.title", equalTo("T"))
                .body("html.body", equalTo("f1=search and f2=query"));
    }

    @Test
    void get_dynamicEmpty_success() {
        when()
                .get("/dynamic/empty")
                .then()
                .statusCode(204);
    }

    @Test
    void get_dynamicRedirecting_statusRedirect() {
        given()
                .redirects().follow(false)
                .when()
                .get("/dynamic/redirecting")
                .then()
                .statusCode(302)
                .header("location", "other");
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

    record SampleParams(int name, String q1, String q2, String f1, String f2) { }
}

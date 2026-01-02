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
                    route("/dynamic/html",
                            getHtml(_ -> "<!doctype html><html><head><title>T</title></head><body>B</body></html>"),
                            postHtml(_ -> "<!doctype html><html><head><title>T</title></head><body>B</body></html>")),
                    route("/dynamic/html/param/{name}",
                            getHtml(SampleParams.class, params -> "<!doctype html><html><head><title>T</title></head><body>" + params.name() + "</body></html>")),
                    route("/dynamic/html/form",
                            postHtml(SampleParams.class, params -> "<!doctype html><html><head><title>T</title></head><body>f1=" + params.f1() + " and f2=" + params.f2() + "</body></html>"))
            ))
            .build();

    private static final HttpServer testee = new HttpServer(config);

    @AfterAll
    static void cleanup() {
        testee.close();
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
    void post_dynamicHtml_success() {
        given()
                .contentType("application/x-www-form-urlencoded; charset=utf-8")
                .when()
                .post("/dynamic/html")
                .then()
                .statusCode(200)
                .contentType("text/html")
                .body("html.head.title", equalTo("T"))
                .body("html.body", equalTo("B"));
    }

    @Test
    void get_dynamicHtmlPathParam_success() {
        when()
                .get("/dynamic/html/param/1234")
                .then()
                .statusCode(200)
                .contentType("text/html")
                .body("html.head.title", equalTo("T"))
                .body("html.body", equalTo("1234"));
    }

    @Test
    void post_dynamicHtmlFromParam_success() {
        given()
                .contentType("application/x-www-form-urlencoded; charset=utf-8")
                .formParam("f1", "search")
                .formParam("f2", "query")
                .when()
                .post("/dynamic/html/form")
                .then()
                .statusCode(200)
                .contentType("text/html")
                .body("html.head.title", equalTo("T"))
                .body("html.body", equalTo("f1=search and f2=query"));
    }

    record SampleParams(int name, String f1, String f2) { }
}

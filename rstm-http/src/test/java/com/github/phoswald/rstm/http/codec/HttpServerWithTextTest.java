package com.github.phoswald.rstm.http.codec;

import static com.github.phoswald.rstm.http.codec.TextCodec.text;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.combine;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.get;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.post;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.route;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import com.github.phoswald.rstm.http.HttpResponse;
import com.github.phoswald.rstm.http.server.HttpServer;
import com.github.phoswald.rstm.http.server.HttpServerConfig;

class HttpServerWithTextTest {

    private static final HttpServerConfig config = HttpServerConfig.builder()
            .httpPort(8080)
            .filter(combine(
                    route("/dynamic/text",
                            get(request -> HttpResponse.body(200, text(), handleGet())),
                            post(request -> HttpResponse.body(200, text(), handlePost(request.body(text(), String.class)))))
            ))
            .build();

    private static final HttpServer testee = new HttpServer(config);

    @AfterAll
    static void cleanup() {
        testee.close();
    }

    @Test
    void get_validJson_success() {
        when()
                .get("/dynamic/text")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(equalTo("Test Output"));
    }

    @Test
    void post_validJson_success() {
        given()
                .contentType("text/plain")
                .body("Test Input")
                .when()
                .post("/dynamic/text")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(equalTo("Test Output for Test Input"));
    }

    private static String handleGet() {
        return "Test Output";
    }

    private static String handlePost(String request) {
        return "Test Output for " + request;
    }
}

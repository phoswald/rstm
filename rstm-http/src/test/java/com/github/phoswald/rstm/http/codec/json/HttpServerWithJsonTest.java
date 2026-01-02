package com.github.phoswald.rstm.http.codec.json;

import static com.github.phoswald.rstm.http.codec.json.JsonCodec.json;
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

class HttpServerWithJsonTest {

    private static final HttpServerConfig config = HttpServerConfig.builder()
            .httpPort(8080)
            .filter(combine(
                    route("/dynamic/json",
                            get(request -> HttpResponse.body(200, json(), handleGet())),
                            post(request -> HttpResponse.body(200, json(), handlePost(request.body(json(), SampleRequest.class)))))
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
                .get("/dynamic/json")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body(equalTo("""
                        {
                            "output": "Test Output"
                        }
                        """));
    }

    @Test
    void post_validJson_success() {
        given()
                .contentType("application/json")
                .body("{\"input\":\"Test Input\"}")
                .when()
                .post("/dynamic/json")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body(equalTo("""
                        {
                            "output": "Test Output for Test Input"
                        }
                        """));
    }

    private static SampleResponse handleGet() {
        return new SampleResponse("Test Output");
    }

    private static SampleResponse handlePost(SampleRequest request) {
        return new SampleResponse("Test Output for " + request.input());
    }
}

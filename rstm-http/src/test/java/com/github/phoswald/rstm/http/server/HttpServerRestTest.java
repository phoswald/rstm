package com.github.phoswald.rstm.http.server;

import static com.github.phoswald.rstm.http.codec.JsonCodec.json;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.combine;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.deleteRest;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.get;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.getHtml;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.getRest;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.postRest;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.putRest;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.route;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import com.github.phoswald.rstm.http.HttpResponse;

class HttpServerRestTest {

    private static final HttpServerConfig config = HttpServerConfig.builder()
            .httpPort(8080)
            .filter(combine(
                    route("/dynamic",
                            getRest(json(), _ -> new SampleResponse("Response for GET")),
                            postRest(json(), SampleRequest.class, request -> new SampleResponse("Response for POST of " + request.message())),
                            putRest(json(), SampleRequest.class, request -> new SampleResponse("Response for PUT of " + request.message())),
                            deleteRest(json(), _ -> new SampleResponse("Response for DELETE"))),
                    route("/dynamic/param/{name}",
                            getRest(json(), SampleParams.class, params -> new SampleResponse("Response for GET with name=" + params.name()))),
                    route("/dynamic/query",
                            getRest(json(), SampleParams.class, params -> new SampleResponse("Response for GET with q1=" + params.q1() + " and q2=" + params.q2())),
                            postRest(json(), SampleParams.class, SampleRequest.class, (params, request) -> new SampleResponse("Response for POST with q1=" + params.q1() + " and q2=" + params.q2() + " of " + request.message())),
                            putRest(json(), SampleParams.class, SampleRequest.class, (params, request) -> new SampleResponse("Response for PUT with q1=" + params.q1() + " and q2=" + params.q2() + " of " + request.message())),
                            deleteRest(json(), SampleParams.class, params -> new SampleResponse("Response for DELETE with q1=" + params.q1() + " and q2=" + params.q2()))),
                    route("/dynamic/form",
                            postRest(json(), SampleParams.class, SampleRequest.class, (params, _) -> new SampleResponse("Response for POST with f1=" + params.f1() + " and f2=" + params.f2())),
                            putRest(json(), SampleParams.class, SampleRequest.class, (params, _) -> new SampleResponse("Response for PUT with f1=" + params.f1() + " and f2=" + params.f2()))),
                    route("/dynamic/empty",
                            getRest(json(),  () -> "")),
                    route("/dynamic/notexisting",
                            getRest(json(), () -> null)),
                    route("/dynamic/failing",
                            getRest(json(), ()-> { throw new IllegalStateException(""); }))
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
                .contentType("application/json")
                .body("message", equalTo("Response for GET"));
    }

    @Test
    void post_dynamic_success() {
        given()
                .contentType("application/json")
                .body("{\"message\":\"Sample Request\"}")
                .when()
                .post("/dynamic")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", equalTo("Response for POST of Sample Request"));
    }

    @Test
    void put_dynamic_success() {
        given()
                .contentType("application/json")
                .body("{\"message\":\"Sample Request\"}")
                .when()
                .put("/dynamic")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", equalTo("Response for PUT of Sample Request"));
    }

    @Test
    void delete_dynamic_success() {
        when()
                .delete("/dynamic")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", equalTo("Response for DELETE"));
    }

    @Test
    void get_dynamicPathParam_success() {
        when()
                .get("/dynamic/param/1234")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", equalTo("Response for GET with name=1234"));
    }

    @Test
    void get_dynamicQueryParam_success() {
        given()
                .queryParam("q1", "search")
                .queryParam("q2", "query")
                .contentType("application/json")
                .body("{\"message\":\"Sample Request\"}")
                .when()
                .get("/dynamic/query")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", equalTo("Response for GET with q1=search and q2=query"));
    }

    @Test
    void post_dynamicQueryParam_success() {
        given()
                .queryParam("q1", "search")
                .queryParam("q2", "query")
                .contentType("application/json")
                .body("{\"message\":\"Sample Request\"}")
                .when()
                .post("/dynamic/query")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", equalTo("Response for POST with q1=search and q2=query of Sample Request"));
    }

    @Test
    void put_dynamicQueryParam_success() {
        given()
                .queryParam("q1", "search")
                .queryParam("q2", "query")
                .contentType("application/json")
                .body("{\"message\":\"Sample Request\"}")
                .when()
                .put("/dynamic/query")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", equalTo("Response for PUT with q1=search and q2=query of Sample Request"));
    }

    @Test
    void delete_dynamicQueryParam_success() {
        given()
                .queryParam("q1", "search")
                .queryParam("q2", "query")
                .contentType("application/json")
                .body("{\"message\":\"Sample Request\"}")
                .when()
                .delete("/dynamic/query")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", equalTo("Response for DELETE with q1=search and q2=query"));
    }

    @Test
    void post_dynamicFormParam_success() {
        given()
                .contentType("application/x-www-form-urlencoded; charset=utf-8")
                .formParam("f1", "search")
                .formParam("f2", "query")
                .when()
                .post("/dynamic/form")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", equalTo("Response for POST with f1=search and f2=query"));
    }

    @Test
    void put_dynamicFormParam_success() {
        given()
                .contentType("application/x-www-form-urlencoded; charset=utf-8")
                .formParam("f1", "search")
                .formParam("f2", "query")
                .when()
                .put("/dynamic/form")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", equalTo("Response for PUT with f1=search and f2=query"));
    }

    @Test
    void get_dynamicEmpty_success() {
        when()
                .get("/dynamic/empty")
                .then()
                .statusCode(204);
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

    record SampleRequest(String message) { }

    record SampleResponse(String message) { }
}

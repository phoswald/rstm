package com.github.phoswald.rstm.http.server;

import static com.github.phoswald.rstm.http.codec.JsonCodec.json;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.combine;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.deleteRest;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.getRest;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.postRest;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.putRest;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.route;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class HttpServerRestTest {

    private static final HttpServerConfig config = HttpServerConfig.builder()
            .httpPort(8080)
            .filter(combine(
                    route("/dynamic/json",
                            getRest(json(), _ -> new SampleResponse("Response for GET")),
                            postRest(json(), SampleRequest.class, request -> new SampleResponse("Response for POST of " + request.message())),
                            putRest(json(), SampleRequest.class, request -> new SampleResponse("Response for PUT of " + request.message())),
                            deleteRest(json(), _ -> new SampleResponse("Response for DELETE"))),
                    route("/dynamic/json/param/{name}",
                            getRest(json(), SampleParams.class, params -> new SampleResponse("Response for GET with name=" + params.name()))),
                    route("/dynamic/json/query",
                            getRest(json(), SampleParams.class, params -> new SampleResponse("Response for GET with q1=" + params.q1() + " and q2=" + params.q2())),
                            postRest(json(), SampleParams.class, SampleRequest.class, (params, request) -> new SampleResponse("Response for POST with q1=" + params.q1() + " and q2=" + params.q2() + " of " + request.message())),
                            putRest(json(), SampleParams.class, SampleRequest.class, (params, request) -> new SampleResponse("Response for PUT with q1=" + params.q1() + " and q2=" + params.q2() + " of " + request.message())),
                            deleteRest(json(), SampleParams.class, (params) -> new SampleResponse("Response for DELETE with q1=" + params.q1() + " and q2=" + params.q2()))),
                    route("/dynamic/json/form",
                            postRest(json(), SampleParams.class, SampleRequest.class, (params, _) -> new SampleResponse("Response for POST with f1=" + params.f1() + " and f2=" + params.f2())),
                            putRest(json(), SampleParams.class, SampleRequest.class, (params, _) -> new SampleResponse("Response for PUT with f1=" + params.f1() + " and f2=" + params.f2())))
            ))
            .build();

    private static final HttpServer testee = new HttpServer(config);

    @AfterAll
    static void cleanup() {
        testee.close();
    }

    @Test
    void get_dynamicJson_success() {
        when()
                .get("/dynamic/json")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", equalTo("Response for GET"));
    }

    @Test
    void post_dynamicJson_success() {
        given()
                .contentType("application/json")
                .body("{\"message\":\"Sample Request\"}")
                .when()
                .post("/dynamic/json")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", equalTo("Response for POST of Sample Request"));
    }

    @Test
    void put_dynamicJson_success() {
        given()
                .contentType("application/json")
                .body("{\"message\":\"Sample Request\"}")
                .when()
                .put("/dynamic/json")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", equalTo("Response for PUT of Sample Request"));
    }

    @Test
    void delete_dynamicJson_success() {
        when()
                .delete("/dynamic/json")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", equalTo("Response for DELETE"));
    }

    @Test
    void get_dynamicJsonPathParam_success() {
        when()
                .get("/dynamic/json/param/1234")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", equalTo("Response for GET with name=1234"));
    }

    @Test
    void get_dynamicJsonQueryParam_success() {
        given()
                .queryParam("q1", "search")
                .queryParam("q2", "query")
                .contentType("application/json")
                .body("{\"message\":\"Sample Request\"}")
                .when()
                .get("/dynamic/json/query")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", equalTo("Response for GET with q1=search and q2=query"));
    }

    @Test
    void post_dynamicJsonQueryParam_success() {
        given()
                .queryParam("q1", "search")
                .queryParam("q2", "query")
                .contentType("application/json")
                .body("{\"message\":\"Sample Request\"}")
                .when()
                .post("/dynamic/json/query")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", equalTo("Response for POST with q1=search and q2=query of Sample Request"));
    }

    @Test
    void put_dynamicJsonQueryParam_success() {
        given()
                .queryParam("q1", "search")
                .queryParam("q2", "query")
                .contentType("application/json")
                .body("{\"message\":\"Sample Request\"}")
                .when()
                .put("/dynamic/json/query")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", equalTo("Response for PUT with q1=search and q2=query of Sample Request"));
    }

    @Test
    void delete_dynamicJsonQueryParam_success() {
        given()
                .queryParam("q1", "search")
                .queryParam("q2", "query")
                .contentType("application/json")
                .body("{\"message\":\"Sample Request\"}")
                .when()
                .delete("/dynamic/json/query")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", equalTo("Response for DELETE with q1=search and q2=query"));
    }

    @Test
    void post_dynamicJsonFormParam_success() {
        given()
                .contentType("application/x-www-form-urlencoded; charset=utf-8")
                .formParam("f1", "search")
                .formParam("f2", "query")
                .when()
                .post("/dynamic/json/form")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", equalTo("Response for POST with f1=search and f2=query"));
    }

    @Test
    void put_dynamicJsonFormParam_success() {
        given()
                .contentType("application/x-www-form-urlencoded; charset=utf-8")
                .formParam("f1", "search")
                .formParam("f2", "query")
                .when()
                .put("/dynamic/json/form")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", equalTo("Response for PUT with f1=search and f2=query"));
    }

    record SampleParams(int name, String q1, String q2, String f1, String f2) { }

    record SampleRequest(String message) { }

    record SampleResponse(String message) { }
}

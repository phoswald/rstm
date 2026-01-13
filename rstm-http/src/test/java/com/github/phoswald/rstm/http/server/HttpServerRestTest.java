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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class HttpServerRestTest {

    private static final HttpServerConfig config = HttpServerConfig.builder()
            .httpPort(8080)
            .filter(combine(
                    route("/dynamic",
                            getRest(json(), SampleResponse.class, _ -> new SampleResponse("Response for GET")),
                            postRest(json(), SampleRequest.class, SampleResponse.class, request -> new SampleResponse("Response for POST of " + request.message())),
                            putRest(json(), SampleRequest.class, SampleResponse.class, request -> new SampleResponse("Response for PUT of " + request.message())),
                            deleteRest(json(), SampleResponse.class, _ -> new SampleResponse("Response for DELETE"))),
                    route("/dynamic/param/{name}",
                            getRest(json(), SampleParams.class, SampleResponse.class, params -> new SampleResponse("Response for GET with name=" + params.name()))),
                    route("/dynamic/query",
                            getRest(json(), SampleParams.class, SampleResponse.class, params -> new SampleResponse("Response for GET with q1=" + params.q1() + " and q2=" + params.q2())),
                            postRest(json(), SampleParams.class, SampleRequest.class, SampleResponse.class, (params, request) -> new SampleResponse("Response for POST with q1=" + params.q1() + " and q2=" + params.q2() + " of " + request.message())),
                            putRest(json(), SampleParams.class, SampleRequest.class, SampleResponse.class, (params, request) -> new SampleResponse("Response for PUT with q1=" + params.q1() + " and q2=" + params.q2() + " of " + request.message())),
                            deleteRest(json(), SampleParams.class, SampleResponse.class, params -> new SampleResponse("Response for DELETE with q1=" + params.q1() + " and q2=" + params.q2()))),
                    route("/dynamic/form",
                            postRest(json(), SampleParams.class, SampleRequest.class, SampleResponse.class, (params, _) -> new SampleResponse("Response for POST with f1=" + params.f1() + " and f2=" + params.f2())),
                            putRest(json(), SampleParams.class, SampleRequest.class, SampleResponse.class, (params, _) -> new SampleResponse("Response for PUT with f1=" + params.f1() + " and f2=" + params.f2()))),
                    route("/dynamic/empty",
                            getRest(json(), String.class, () -> "")),
                    route("/dynamic/notexisting",
                            getRest(json(), SampleResponse.class, () -> null)),
                    route("/dynamic/failing",
                            getRest(json(), SampleResponse.class, ()-> { throw new IllegalStateException(""); }))
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

    @Test
    void createMetadata_valid_success() {
        List<RouteMetadata> routes = config.filter().createMetadata();
        assertThat(routes.size(), Matchers.greaterThanOrEqualTo(14));
        assertTrue(routes.stream().allMatch(r -> r.route().startsWith("/dynamic")));
        assertTrue(routes.stream().allMatch(r -> r.method() != null));
        assertTrue(routes.stream().anyMatch(r -> r.pathParams().contains("name")));
        assertTrue(routes.stream().anyMatch(r -> r.contentType().equals("application/json")));
        assertTrue(routes.stream().anyMatch(r -> r.requestClass() != null));
        assertTrue(routes.stream().anyMatch(r -> r.responseClass() != null));
    }

    record SampleParams(int name, String q1, String q2, String f1, String f2) { }

    record SampleRequest(String message) { }

    record SampleResponse(String message) { }
}

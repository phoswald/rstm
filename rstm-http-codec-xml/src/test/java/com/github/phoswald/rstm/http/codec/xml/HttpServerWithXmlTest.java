package com.github.phoswald.rstm.http.codec.xml;

import static com.github.phoswald.rstm.http.codec.xml.XmlCodec.xml;
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

class HttpServerWithXmlTest {

    private static final HttpServerConfig config = HttpServerConfig.builder() //
            .httpPort(8080) //
            .filter(combine( //
                    route("/dynamic/xml", //
                            get(request -> HttpResponse.body(200, xml(), handleGet())), //
                            post(request -> HttpResponse.body(200, xml(), handlePost(request.body(xml(), SampleRequest.class))))) //
            )) //
            .build();

    private static final HttpServer testee = new HttpServer(config);

    @AfterAll
    static void cleanup() {
        testee.close();
    }

    @Test
    void get_validXml_success() {
        when().
            get("/dynamic/xml").
        then().
            statusCode(200).
            contentType("text/xml").
            body(equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<sampleResponse>\n    <output>Test Output</output>\n</sampleResponse>\n"));
    }

    @Test
    void post_validXml_success() {
        given().
            contentType("text/xml").
            body("<sampleRequest><input>Test Input</input></sampleRequest>").
        when().
            post("/dynamic/xml").
        then().
            statusCode(200).
            contentType("text/xml").
            body(equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<sampleResponse>\n    <output>Test Output for Test Input</output>\n</sampleResponse>\n"));
    }

    private static SampleResponse handleGet() {
        SampleResponse response = new SampleResponse();
        response.setOutput("Test Output");
        return response;
    }

    private static SampleResponse handlePost(SampleRequest request) {
        SampleResponse response = new SampleResponse();
        response.setOutput("Test Output for " + request.getInput());
        return response;
    }
}

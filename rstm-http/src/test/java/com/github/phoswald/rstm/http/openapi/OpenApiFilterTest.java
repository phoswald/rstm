package com.github.phoswald.rstm.http.openapi;

import static com.github.phoswald.rstm.http.codec.JsonCodec.json;
import static com.github.phoswald.rstm.http.codec.TextCodec.text;
import static com.github.phoswald.rstm.http.codec.XmlCodec.xml;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.auth;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.combine;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.deleteRest;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.get;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.getHtml;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.getRest;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.post;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.postHtml;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.postRest;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.putRest;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.resources;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.route;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.phoswald.rstm.http.HttpResponse;
import com.github.phoswald.rstm.http.server.HttpFilter;
import com.github.phoswald.rstm.http.server.HttpServer;
import com.github.phoswald.rstm.http.server.HttpServerConfig;

class OpenApiFilterTest {

    OpenApiConfig openApiConfig = new OpenApiConfigBuilder()
            .title("rstm")
            .description("OpenAPI for RSTM Unit Test")
            .version("1.0.0")
            .urls(List.of("http://localhost:8080"))
            .build();
    private final OpenApiFilter testee = new OpenApiFilter(openApiConfig, getRoutes());

    private final HttpServerConfig serverConfig = HttpServerConfig.builder()
            .httpPort(8080)
            .filter(testee)
            .build();

    @Test
    void generateOpenApiSpecJson_valid_success() throws IOException {
        String json = testee.generateOpenApiSpecJson();
        String expectedJson = Files.readString(Path.of("src/test/resources/openapi.json"));
        assertEquals(expectedJson, json);
    }

    @Test
    void getOpenApi_valid_success()  {
        try(HttpServer _ = new HttpServer(serverConfig)) {
            when()
                    .get("/openapi")
                    .then()
                    .statusCode(200)
                    .contentType("application/json")
                    .body(containsString("\"openapi\": \"3.0.0\""));
        }
    }

    @Test
    void getOpenApiUi_valid_success() {
        try(HttpServer _ = new HttpServer(serverConfig)) {
            when()
                    .get("/openapi/ui")
                    .then()
                    .statusCode(200)
                    .contentType("text/html")
                    .body(containsString("<title>OpenAPI</title>"));
            }
    }

    private HttpFilter getRoutes() {
        return combine(
                resources("/html/"),
                route("/dynamic",
                        get(_ -> HttpResponse.text(200, "Response")),
                        post(_ -> HttpResponse.text(200, "Response"))),
                route("/app/rest/sample/time",
                        getRest(text(), String.class, () -> null)),
                route("/app/rest/sample/config",
                        getRest(text(), String.class, () -> null)),
                route("/app/rest/sample/echo-xml",
                        postRest(xml(), EchoRequest.class, EchoResponse.class, _ -> null)),
                route("/app/rest/sample/echo-json",
                        postRest(json(), EchoRequest.class, EchoResponse.class, _ -> null)),
                route("/app/rest/sample/me", auth("user",
                        getRest(text(), String.class, _ -> null))),
                route("/app/rest/tasks",
                        getRest(json(), TaskList.class, () -> null),
                        postRest(json(), Task.class, Task.class, _ -> null)),
                route("/app/rest/tasks/{id}",
                        getRest(json(), IdParams.class, Task.class, _ -> null),
                        putRest(json(), IdParams.class, Task.class, Task.class, (_,_) -> null),
                        deleteRest(json(), IdParams.class, String.class, _ -> null)),
                route("/app/pages", auth("user",
                        route("/sample",
                                getHtml(_ -> null)),
                        route("/tasks",
                                getHtml(() -> null),
                                postHtml(PostParams.class, _ -> null)),
                        route("/tasks/{id}",
                                getHtml(IdParams.class, _ -> null),
                                postHtml(IdPostParams.class, _ -> null))))
        );
    }

    record EchoRequest(String input) { }

    record EchoResponse(String output) { }

    record TaskList(List<Task> tasks) { }

    record Task(
            String taskId,
            String userId,
            Instant timestamp,
            String title,
            String description,
            boolean done
    ) { }

    record IdParams(String id) { }

    record PostParams(String title, String description) { }

    record IdPostParams(String id, String title, String description, String done) { }
}

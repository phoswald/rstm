package com.github.phoswald.rstm.http.openapi;

import static com.github.phoswald.rstm.http.codec.JsonCodec.json;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.combine;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.getHtml;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.getRest;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.route;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.server.HttpFilter;
import com.github.phoswald.rstm.http.server.HttpServerConfig;

public class OpenApiProvider {

    private final OpenApiConfig config;

    public OpenApiProvider(OpenApiConfig config) {
        this.config = config;
    }

    public HttpFilter createRoutes() {
        return combine(
                route("/openapi", getRest(json(), Object.class, this::generateOpenApiSpec)),
                route("/openapi/ui", getHtml(this::generateOpenApiUiPage)));
    }

    Object generateOpenApiSpec(HttpRequest request) {
        return createFactory(request.config()).generateOpenApiSpec();
    }

    String generateOpenApiSpecJson(HttpServerConfig serverConfig) {
        return createFactory(serverConfig).generateOpenApiSpecJson();
    }

    private OpenApiSpecFactory createFactory(HttpServerConfig serverConfig) {
        return new OpenApiSpecFactory(config, serverConfig.filter());
    }

    private String generateOpenApiUiPage() throws IOException {
        try(var reader = new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/html/openapi-ui.html")))) {
            return reader.readAllAsString();
        }
    }
}

package com.github.phoswald.rstm.http.openapi;

import static com.github.phoswald.rstm.http.codec.JsonCodec.json;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.combine;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.getHtml;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.getRest;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.route;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;
import com.github.phoswald.rstm.http.server.HttpFilter;
import com.github.phoswald.rstm.http.server.HttpServerConfig;
import com.github.phoswald.rstm.http.server.RouteMetadata;

public class OpenApiFilter implements HttpFilter {

    private final OpenApiConfig config;
    private final HttpFilter filter;

    public OpenApiFilter(OpenApiConfig config, HttpFilter filter) {
        this.config = config;
        this.filter = combine(filter, createRoute());
    }

    @Override
    public HttpResponse handle(String path, HttpRequest request, HttpServerConfig config) throws Exception {
        return filter.handle(path, request, config);
    }

    @Override
    public List<RouteMetadata> createMetadata() {
        return filter.createMetadata();
    }

    private HttpFilter createRoute() {
        return combine(
                route("/openapi", getRest(json(), Object.class, _ -> createFactory().generateOpenApiSpec())),
                route("/openapi/ui", getHtml(this::generateOpenApiUiPage)));
    }

    String generateOpenApiSpecJson() {
        return createFactory().generateOpenApiSpecJson();
    }

    private OpenApiSpecFactory createFactory() {
        return new OpenApiSpecFactory(config, filter);
    }

    private String generateOpenApiUiPage() throws IOException {
        try(var reader = new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/html/openapi-ui.html")))) {
            return reader.readAllAsString();
        }
    }
}

package com.github.phoswald.rstm.http.server;

import java.nio.file.Path;

import com.github.phoswald.record.builder.RecordBuilder;
import com.github.phoswald.rstm.http.HttpMethod;
import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;

@RecordBuilder
public record HttpServerConfig( //
        int httpPort, //
        HttpFilter handler) {

    public static HttpServerConfigBuilder builder() {
        return new HttpServerConfigBuilder();
    }

    public static HttpFilter resources(String baseResource) {
        return new ResourcesHandler(baseResource);
    }

    public static HttpFilter filesystem(Path basePath) {
        return new FilesystemHandler(basePath);
    }

    public static HttpFilter all(HttpFilter... handlers) {
        return (path, request) -> {
            for (HttpFilter handler : handlers) {
                HttpResponse resoponse = handler.handle(path, request);
                if (resoponse != null) {
                    return resoponse;
                }
            }
            return null;
        };
    }

    public static HttpFilter route(String route, HttpFilter handler) {
        return (path, request) -> {
            if (path.startsWith(route)) {
                return handler.handle(path.substring(route.length()), request);
            } else {
                return null;
            }
        };
    }

    public static HttpFilter get(ThrowingFunction<HttpRequest, HttpResponse> handler) {
        return (path, request) -> {
            if (request.method() == HttpMethod.GET) {
                return handler.invoke(request);
            } else {
                return null;
            }
        };
    }
}

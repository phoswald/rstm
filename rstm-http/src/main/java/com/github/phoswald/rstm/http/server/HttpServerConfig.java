package com.github.phoswald.rstm.http.server;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.phoswald.record.builder.RecordBuilder;
import com.github.phoswald.rstm.http.HttpMethod;
import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;

@RecordBuilder
public record HttpServerConfig( //
        int httpPort, //
        HttpFilter handler //
) {

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

    public static HttpFilter routePattern(String route, HttpFilter handler) {
        // TODO: correctly handle pattern not covering whole path
        Pattern pattern = Pattern.compile("^" + route + "$");
        return (path, request) -> {
            Matcher matcher = pattern.matcher(path);
            if (matcher.matches()) {
                Map<String, String> params = new HashMap<>(request.pathParams());
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    params.put(Integer.toString(params.size() + 1), matcher.group(i));
                }
                return handler.handle("", request.toBuilder().pathParams(params).build());
            } else {
                return null;
            }
        };
    }

    public static HttpFilter get(ThrowingFunction<HttpRequest, HttpResponse> handler) {
        return method(HttpMethod.GET, handler);
    }


    public static HttpFilter post(ThrowingFunction<HttpRequest, HttpResponse> handler) {
        return method(HttpMethod.POST, handler);
    }

    public static HttpFilter put(ThrowingFunction<HttpRequest, HttpResponse> handler) {
        return method(HttpMethod.PUT, handler);
    }

    private static HttpFilter method(HttpMethod method, ThrowingFunction<HttpRequest, HttpResponse> handler) {
        return (path, request) -> {
            if (request.method() == method) {
                return handler.invoke(request);
            } else {
                return null;
            }
        };
    }
}

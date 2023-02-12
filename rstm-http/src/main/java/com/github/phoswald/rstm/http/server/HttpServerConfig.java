package com.github.phoswald.rstm.http.server;

import java.nio.file.Path;
import java.util.Arrays;

import com.github.phoswald.record.builder.RecordBuilder;
import com.github.phoswald.rstm.http.HttpMethod;
import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;

@RecordBuilder
public record HttpServerConfig( //
        int httpPort, //
        HttpFilter filter //
) {

    public static HttpServerConfigBuilder builder() {
        return new HttpServerConfigBuilder();
    }

    public static HttpFilter resources(String baseResource) {
        return new ResourcesFilter(baseResource);
    }

    public static HttpFilter filesystem(Path basePath) {
        return new FilesystemFilter(basePath);
    }

    public static HttpFilter route(String route, HttpFilter... filters) {
        return new RouteFilter(route, combine(filters));
    }

    public static HttpFilter get(ThrowingFunction<HttpRequest, HttpResponse> filter) {
        return method(HttpMethod.GET, filter);
    }

    public static HttpFilter post(ThrowingFunction<HttpRequest, HttpResponse> filter) {
        return method(HttpMethod.POST, filter);
    }

    public static HttpFilter put(ThrowingFunction<HttpRequest, HttpResponse> filter) {
        return method(HttpMethod.PUT, filter);
    }

    public static HttpFilter delete(ThrowingFunction<HttpRequest, HttpResponse> filter) {
        return method(HttpMethod.DELETE, filter);
    }

    private static HttpFilter method(HttpMethod method, ThrowingFunction<HttpRequest, HttpResponse> filter) {
        return new MethodFilter(method, filter);
    }

    public static HttpFilter combine(HttpFilter... filters) {
        if(filters.length == 1) {
            return filters[0];
        } else {
            return new CombineFilter(Arrays.asList(filters));
        }
    }
}

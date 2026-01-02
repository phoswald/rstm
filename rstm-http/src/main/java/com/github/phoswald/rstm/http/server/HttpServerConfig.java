package com.github.phoswald.rstm.http.server;

import java.nio.file.Path;
import java.util.List;

import com.github.phoswald.record.builder.RecordBuilder;
import com.github.phoswald.rstm.http.HttpCodec;
import com.github.phoswald.rstm.http.HttpMethod;
import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;
import com.github.phoswald.rstm.security.IdentityProvider;

@RecordBuilder
public record HttpServerConfig(
        int httpPort,
        HttpFilter filter,
        IdentityProvider identityProvider
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

    public static HttpFilter auth(String role, HttpFilter... filters) {
        return new AuthFilter(List.of(role), combine(filters));
    }

    public static HttpFilter login() {
        return new LoginFilter();
    }

    public static HttpFilter oidc() {
        return new OidcFilter();
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

    public static <T> HttpFilter getRest(HttpCodec codec, ThrowingFunction<HttpRequest, T> handler) {
        return get(request -> {
            T responseObj = handler.invoke(request);
            return responseObj == null ? HttpResponse.empty(404) : HttpResponse.body(200, codec, responseObj);
        });
    }

    public static <A, B> HttpFilter postRest(HttpCodec codec, Class<A> clazzA, ThrowingBiFunction<HttpRequest, A, B> handler) {
        return post(request -> {
            A requestObj = request.body(codec, clazzA);
            B responseObj = handler.invoke(request, requestObj);
            return responseObj == null ? HttpResponse.empty(404) : HttpResponse.body(200, codec, responseObj);
        });
    }

    public static <A, B> HttpFilter putRest(HttpCodec codec, Class<A> clazzA, ThrowingBiFunction<HttpRequest, A, B> handler) {
        return put(request -> {
            A requestObj = request.body(codec, clazzA);
            B responseObj = handler.invoke(request, requestObj);
            return responseObj == null ? HttpResponse.empty(404) : HttpResponse.body(200, codec, responseObj);
        });
    }

    public static HttpFilter getHtml(ThrowingFunction<HttpRequest, Object> handler) {
        return get(request -> {
            Object response = handler.invoke(request);
            return HttpResponse.html(200, response.toString());
        });
    }

    public static HttpFilter postHtml(ThrowingFunction<HttpRequest, Object> handler) {
        return post(request -> {
            Object response = handler.invoke(request);
            if (response instanceof Path location) {
                return HttpResponse.redirect(302, request.relativizePath(location.toString()));
            } else {
                return HttpResponse.html(200, response.toString());
            }
        });
    }

    private static HttpFilter method(HttpMethod method, ThrowingFunction<HttpRequest, HttpResponse> filter) {
        return new MethodFilter(method, filter);
    }

    public static HttpFilter combine(HttpFilter... filters) {
        if (filters.length == 1) {
            return filters[0];
        } else {
            return new CombineFilter(List.of(filters));
        }
    }
}

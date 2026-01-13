package com.github.phoswald.rstm.http.server;

import static com.github.phoswald.rstm.http.HttpMethod.DELETE;
import static com.github.phoswald.rstm.http.HttpMethod.GET;
import static com.github.phoswald.rstm.http.HttpMethod.POST;
import static com.github.phoswald.rstm.http.HttpMethod.PUT;

import java.nio.file.Path;
import java.util.List;

import com.github.phoswald.record.builder.RecordBuilder;
import com.github.phoswald.rstm.http.HttpCodec;
import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;
import com.github.phoswald.rstm.http.openapi.OpenApiConfig;
import com.github.phoswald.rstm.http.openapi.OpenApiFilter;
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

    public static HttpFilter openapi(OpenApiConfig config, HttpFilter... filters) {
        return new OpenApiFilter(config, combine(filters));
    }

    public static HttpFilter get(ThrowingFunction<HttpRequest, HttpResponse> handler) {
        return MethodFilter.of(GET, handler);
    }

    public static HttpFilter post(ThrowingFunction<HttpRequest, HttpResponse> handler) {
        return MethodFilter.of(POST, handler);
    }

    public static HttpFilter put(ThrowingFunction<HttpRequest, HttpResponse> handler) {
        return MethodFilter.of(PUT, handler);
    }

    public static HttpFilter delete(ThrowingFunction<HttpRequest, HttpResponse> handler) {
        return MethodFilter.of(DELETE, handler);
    }

    public static HttpFilter getHtml(ThrowingSupplier<String> handler) {
        return MethodFilter.forHtml(GET, ignoreRequest(handler));
    }

    public static HttpFilter getHtml(ThrowingFunction<HttpRequest, String> handler) {
        return MethodFilter.forHtml(GET, handler);
    }

    public static <PAR> HttpFilter getHtml(Class<PAR> clazzPar, ThrowingFunction<PAR, String> handler) {
        return MethodFilter.forHtmlWithParams(GET, clazzPar, ignoreRequest(handler));
    }

    public static <PAR> HttpFilter getHtml(Class<PAR> clazzPar, ThrowingBiFunction<HttpRequest, PAR, String> handler) {
        return MethodFilter.forHtmlWithParams(GET, clazzPar, handler);
    }

    public static HttpFilter postHtml(ThrowingSupplier<String> handler) {
        return MethodFilter.forHtml(POST, ignoreRequest(handler));
    }

    public static HttpFilter postHtml(ThrowingFunction<HttpRequest, String> handler) {
        return MethodFilter.forHtml(POST, handler);
    }

    public static <PAR> HttpFilter postHtml(Class<PAR> clazzPar, ThrowingFunction<PAR, String> handler) {
        return MethodFilter.forHtmlWithParams(POST, clazzPar, ignoreRequest(handler));
    }

    public static <PAR> HttpFilter postHtml(Class<PAR> clazzPar, ThrowingBiFunction<HttpRequest, PAR, String> handler) {
        return MethodFilter.forHtmlWithParams(POST, clazzPar, handler);
    }

    public static <RES> HttpFilter getRest(HttpCodec codec, Class<RES> clazzRes, ThrowingSupplier<RES> handler) {
        return MethodFilter.forRestWithResponse(GET, codec, clazzRes, ignoreRequest(handler));
    }

    public static <RES> HttpFilter getRest(HttpCodec codec, Class<RES> clazzRes, ThrowingFunction<HttpRequest, RES> handler) {
        return MethodFilter.forRestWithResponse(GET, codec, clazzRes, handler);
    }

    public static <PAR, RES> HttpFilter getRest(HttpCodec codec, Class<PAR> clazzPar, Class<RES> clazzRes, ThrowingFunction<PAR, RES> handler) {
        return MethodFilter.forRestWithParamsResponse(GET, codec, clazzPar, clazzRes, ignoreRequest(handler));
    }

    public static <PAR, RES> HttpFilter getRest(HttpCodec codec, Class<PAR> clazzPar, Class<RES> clazzRes, ThrowingBiFunction<HttpRequest, PAR, RES> handler) {
        return MethodFilter.forRestWithParamsResponse(GET, codec, clazzPar, clazzRes, handler);
    }

    public static <REQ, RES> HttpFilter postRest(HttpCodec codec, Class<REQ> clazzReq, Class<RES> clazzRes, ThrowingFunction<REQ, RES> handler) {
        return MethodFilter.forRestWithRequestResponse(POST, codec, clazzReq, clazzRes, ignoreRequest(handler));
    }

    public static <REQ, RES> HttpFilter postRest(HttpCodec codec, Class<REQ> clazzReq, Class<RES> clazzRes, ThrowingBiFunction<HttpRequest, REQ, RES> handler) {
        return MethodFilter.forRestWithRequestResponse(POST, codec, clazzReq, clazzRes,handler);
    }

    public static <PAR, REQ, RES> HttpFilter postRest(HttpCodec codec, Class<PAR> clazzPar, Class<REQ> clazzReq, Class<RES> clazzRes, ThrowingBiFunction<PAR, REQ, RES> handler) {
        return MethodFilter.forRestWithParamsRequestResponse(POST, codec, clazzPar, clazzReq, clazzRes, ignoreRequest(handler));
    }

    public static <PAR, REQ, RES> HttpFilter postRest(HttpCodec codec, Class<PAR> clazzPar, Class<REQ> clazzReq, Class<RES> clazzRes, ThrowingTriFunction<HttpRequest, PAR, REQ, RES> handler) {
        return MethodFilter.forRestWithParamsRequestResponse(POST, codec, clazzPar, clazzReq, clazzRes, handler);
    }

    public static <REQ, RES> HttpFilter putRest(HttpCodec codec, Class<REQ> clazzReq, Class<RES> clazzRes, ThrowingFunction<REQ, RES> handler) {
        return MethodFilter.forRestWithRequestResponse(PUT, codec, clazzReq, clazzRes, ignoreRequest(handler));
    }

    public static <REQ, RES> HttpFilter putRest(HttpCodec codec, Class<REQ> clazzReq, Class<RES> clazzRes, ThrowingBiFunction<HttpRequest, REQ, RES> handler) {
        return MethodFilter.forRestWithRequestResponse(PUT, codec, clazzReq, clazzRes, handler);
    }

    public static <PAR, REQ, RES> HttpFilter putRest(HttpCodec codec, Class<PAR> clazzPar, Class<REQ> clazzReq, Class<RES> clazzRes, ThrowingBiFunction<PAR, REQ, RES> handler) {
        return MethodFilter.forRestWithParamsRequestResponse(PUT, codec, clazzPar, clazzReq, clazzRes, ignoreRequest(handler));
    }

    public static <PAR, REQ, RES> HttpFilter putRest(HttpCodec codec, Class<PAR> clazzPar, Class<REQ> clazzReq, Class<RES> clazzRes, ThrowingTriFunction<HttpRequest, PAR, REQ, RES> handler) {
        return MethodFilter.forRestWithParamsRequestResponse(PUT, codec, clazzPar, clazzReq, clazzRes, handler);
    }

    public static <RES> HttpFilter deleteRest(HttpCodec codec, Class<RES> clazzRes, ThrowingSupplier<RES> handler) {
        return MethodFilter.forRestWithResponse(DELETE, codec, clazzRes, ignoreRequest(handler));
    }

    public static <RES> HttpFilter deleteRest(HttpCodec codec, Class<RES> clazzRes, ThrowingFunction<HttpRequest, RES> handler) {
        return MethodFilter.forRestWithResponse(DELETE, codec, clazzRes, handler);
    }

    public static <PAR, RES> HttpFilter deleteRest(HttpCodec codec, Class<PAR> clazzPar, Class<RES> clazzRes, ThrowingFunction<PAR, RES> handler) {
        return MethodFilter.forRestWithParamsResponse(DELETE, codec, clazzPar, clazzRes, ignoreRequest(handler));
    }

    public static <PAR, RES> HttpFilter deleteRest(HttpCodec codec, Class<PAR> clazzPar, Class<RES> clazzRes, ThrowingBiFunction<HttpRequest, PAR, RES> handler) {
        return MethodFilter.forRestWithParamsResponse(DELETE, codec, clazzPar, clazzRes, handler);
    }

    private static <A> ThrowingFunction<HttpRequest, A> ignoreRequest(ThrowingSupplier<A> handler) {
        return (_) -> handler.invoke();
    }

    private static <A, B> ThrowingBiFunction<HttpRequest, A, B> ignoreRequest(ThrowingFunction<A, B> handler) {
        return (_, paramA) -> handler.invoke(paramA);
    }

    private static <A, B, C> ThrowingTriFunction<HttpRequest, A, B, C> ignoreRequest(ThrowingBiFunction<A, B, C> handler) {
        return (_, paramA, paramB) -> handler.invoke(paramA, paramB);
    }

    public static HttpFilter combine(HttpFilter... filters) {
        if (filters.length == 1) {
            return filters[0];
        } else {
            return new CombineFilter(List.of(filters));
        }
    }
}

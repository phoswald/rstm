package com.github.phoswald.rstm.http.server;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.phoswald.record.builder.RecordBuilder;
import com.github.phoswald.rstm.databind.Databinder;
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

    private static final Databinder BINDER = new Databinder();

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

    private static HttpFilter method(HttpMethod method, ThrowingFunction<HttpRequest, HttpResponse> filter) {
        return new MethodFilter(method, filter);
    }

    public static HttpFilter getHtml(ThrowingSupplier<String> handler) {
        return getHtml(ignoreRequest(handler));
    }

    public static HttpFilter getHtml(ThrowingFunction<HttpRequest, String> handler) {
        return get(request -> {
            String response = handler.invoke(request);
            return createHtmlResponse(request, response);
        });
    }

    public static <PAR> HttpFilter getHtml(Class<PAR> clazzPar, ThrowingFunction<PAR, String> handler) {
        return getHtml(clazzPar, ignoreRequest(handler));
    }

    public static <PAR> HttpFilter getHtml(Class<PAR> clazzPar, ThrowingBiFunction<HttpRequest, PAR, String> handler) {
        return get(request -> {
            PAR paramsObj = params(request, clazzPar);
            String response = handler.invoke(request, paramsObj);
            return createHtmlResponse(request, response);
        });
    }

    public static HttpFilter postHtml(ThrowingSupplier<String> handler) {
        return postHtml(ignoreRequest(handler));
    }

    public static HttpFilter postHtml(ThrowingFunction<HttpRequest, String> handler) {
        return post(request -> {
            String response = handler.invoke(request);
            return createHtmlResponse(request, response);
        });
    }

    public static <PAR> HttpFilter postHtml(Class<PAR> clazzPar, ThrowingFunction<PAR, String> handler) {
        return postHtml(clazzPar, ignoreRequest(handler));
    }

    public static <PAR> HttpFilter postHtml(Class<PAR> clazzPar, ThrowingBiFunction<HttpRequest, PAR, String> handler) {
        return post(request -> {
            PAR paramsObj = params(request, clazzPar);
            String response = handler.invoke(request, paramsObj);
            return createHtmlResponse(request, response);
        });
    }

    private static HttpResponse createHtmlResponse(HttpRequest request, String response) {
        if(response == null) {
            return HttpResponse.empty(404);
        } else if (response.isEmpty()) {
            return HttpResponse.empty(204);
        } else if (response.startsWith("redirect=")) {
            return HttpResponse.redirect(302, request.relativizePath(response.substring(9)));
        } else {
            return HttpResponse.html(200, response.toString());
        }
    }

    public static <RES> HttpFilter getRest(HttpCodec codec, ThrowingSupplier<RES> handler) {
        return getRest(codec, ignoreRequest(handler));
    }

    public static <RES> HttpFilter getRest(HttpCodec codec, ThrowingFunction<HttpRequest, RES> handler) {
        return get(request -> {
            RES responseObj = handler.invoke(request);
            return createRestResponse(codec, responseObj);
        });
    }

    public static <PAR, RES> HttpFilter getRest(HttpCodec codec, Class<PAR> clazzPar, ThrowingFunction<PAR, RES> handler) {
        return getRest(codec, clazzPar, ignoreRequest(handler));
    }

    public static <PAR, RES> HttpFilter getRest(HttpCodec codec, Class<PAR> clazzPar, ThrowingBiFunction<HttpRequest, PAR, RES> handler) {
        return get(request -> {
            PAR paramsObj = params(request, clazzPar);
            RES responseObj = handler.invoke(request, paramsObj);
            return createRestResponse(codec, responseObj);
        });
    }

    public static <REQ, RES> HttpFilter postRest(HttpCodec codec, Class<REQ> clazzReq, ThrowingFunction<REQ, RES> handler) {
        return postRest(codec, clazzReq, ignoreRequest(handler));
    }

    public static <REQ, RES> HttpFilter postRest(HttpCodec codec, Class<REQ> clazzReq, ThrowingBiFunction<HttpRequest, REQ, RES> handler) {
        return post(request -> {
            REQ requestObj = request.body(codec, clazzReq);
            RES responseObj = handler.invoke(request, requestObj);
            return createRestResponse(codec, responseObj);
        });
    }

    public static <PAR, REQ, RES> HttpFilter postRest(HttpCodec codec, Class<PAR> clazzPar, Class<REQ> clazzReq, ThrowingBiFunction<PAR, REQ, RES> handler) {
        return postRest(codec, clazzPar, clazzReq, ignoreRequest(handler));
    }

    public static <PAR, REQ, RES> HttpFilter postRest(HttpCodec codec, Class<PAR> clazzPar, Class<REQ> clazzReq, ThrowingTriFunction<HttpRequest, PAR, REQ, RES> handler) {
        return post(request -> {
            PAR paramsObj = params(request, clazzPar);
            REQ requestObj = request.body(codec, clazzReq);
            RES responseObj = handler.invoke(request, paramsObj, requestObj);
            return createRestResponse(codec, responseObj);
        });
    }

    public static <REQ, RES> HttpFilter putRest(HttpCodec codec, Class<REQ> clazzReq, ThrowingFunction<REQ, RES> handler) {
        return putRest(codec, clazzReq, ignoreRequest(handler));
    }

    public static <REQ, RES> HttpFilter putRest(HttpCodec codec, Class<REQ> clazzReq, ThrowingBiFunction<HttpRequest, REQ, RES> handler) {
        return put(request -> {
            REQ requestObj = request.body(codec, clazzReq);
            RES responseObj = handler.invoke(request, requestObj);
            return createRestResponse(codec, responseObj);
        });
    }

    public static <PAR, REQ, RES> HttpFilter putRest(HttpCodec codec, Class<PAR> clazzPar, Class<REQ> clazzReq, ThrowingBiFunction<PAR, REQ, RES> handler) {
        return putRest(codec, clazzPar, clazzReq, ignoreRequest(handler));
    }

    public static <PAR, REQ, RES> HttpFilter putRest(HttpCodec codec, Class<PAR> clazzPar, Class<REQ> clazzReq, ThrowingTriFunction<HttpRequest, PAR, REQ, RES> handler) {
        return put(request -> {
            PAR paramsObj = params(request, clazzPar);
            REQ requestObj = request.body(codec, clazzReq);
            RES responseObj = handler.invoke(request, paramsObj, requestObj);
            return createRestResponse(codec, responseObj);
        });
    }

    public static <RES> HttpFilter deleteRest(HttpCodec codec, ThrowingSupplier<RES> handler) {
        return deleteRest(codec, ignoreRequest(handler));
    }

    public static <RES> HttpFilter deleteRest(HttpCodec codec, ThrowingFunction<HttpRequest, RES> handler) {
        return delete(request -> {
            RES responseObj = handler.invoke(request);
            return createRestResponse(codec, responseObj);
        });
    }

    public static <PAR, RES> HttpFilter deleteRest(HttpCodec codec, Class<PAR> clazzPar, ThrowingFunction<PAR, RES> handler) {
        return deleteRest(codec, clazzPar, ignoreRequest(handler));
    }

    public static <PAR, RES> HttpFilter deleteRest(HttpCodec codec, Class<PAR> clazzPar, ThrowingBiFunction<HttpRequest, PAR, RES> handler) {
        return delete(request -> {
            PAR paramsObj = params(request, clazzPar);
            RES responseObj = handler.invoke(request, paramsObj);
            return createRestResponse(codec, responseObj);
        });
    }

    private static <RES> HttpResponse createRestResponse(HttpCodec codec, RES responseObj) {
        if(responseObj == null) {
            return HttpResponse.empty(404);
        } else if(responseObj instanceof  String responeStr && responeStr.isEmpty()) {
            return HttpResponse.empty(204);
        } else {
            return HttpResponse.body(200, codec, responseObj);
        }
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

    private static <PAR> PAR params(HttpRequest request, Class<PAR> clazz) {
        Map<String, Object> map = new HashMap<>();
        map.putAll(request.queryParams()); // lowest prio
        map.putAll(request.formParams()); // overrides query params
        map.putAll(request.pathParams()); // overrides form params and query params
        return BINDER.createInstance(clazz, map);
    }

    public static HttpFilter combine(HttpFilter... filters) {
        if (filters.length == 1) {
            return filters[0];
        } else {
            return new CombineFilter(List.of(filters));
        }
    }
}

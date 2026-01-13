package com.github.phoswald.rstm.http.server;

import static com.github.phoswald.rstm.http.HttpConstants.CONTENT_TYPE_HTML;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.phoswald.rstm.databind.Databinder;
import com.github.phoswald.rstm.http.HttpCodec;
import com.github.phoswald.rstm.http.HttpMethod;
import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;

class MethodFilter implements HttpFilter {

    private static final Databinder BINDER = new Databinder();

    private final HttpMethod method;
    private final ThrowingFunction<HttpRequest, HttpResponse> handler;
    private final RouteMetadata routeMetadata;

    private MethodFilter(HttpMethod method, ThrowingFunction<HttpRequest, HttpResponse> handler) {
        this(method, null, null, null, handler);
    }

    private MethodFilter(HttpMethod method, String contentType, Class<?> clazzReq, Class<?> clazzRes, ThrowingFunction<HttpRequest, HttpResponse> handler) {
        this.method = method;
        this.handler = handler;
        this.routeMetadata = RouteMetadata.forMethod(method, contentType, clazzReq, clazzRes);
    }

    @Override
    public HttpResponse handle(String path, HttpRequest request, HttpServerConfig config) throws Exception {
        if (request.method() == method && path.isEmpty()) {
            return handler.invoke(request);
        } else {
            return null;
        }
    }

    @Override
    public List<RouteMetadata> createMetadata() {
        return List.of(routeMetadata);
    }

    static HttpFilter of(HttpMethod method, ThrowingFunction<HttpRequest, HttpResponse> handler) {
        return new MethodFilter(method, handler);
    }

    static HttpFilter forHtml(HttpMethod method, ThrowingFunction<HttpRequest, String> handler) {
        return new MethodFilter(method, CONTENT_TYPE_HTML, null, String.class, request -> {
            String response = handler.invoke(request);
            return createHtmlResponse(request, response);
        });
    }

    static <PAR> HttpFilter forHtmlWithParams(HttpMethod method, Class<PAR> clazzPar, ThrowingBiFunction<HttpRequest, PAR, String> handler) {
        return new MethodFilter(method, CONTENT_TYPE_HTML, null, String.class, request -> {
            PAR paramsObj = createParams(request, clazzPar);
            String response = handler.invoke(request, paramsObj);
            return createHtmlResponse(request, response);
        });
    }

    static <RES> HttpFilter forRestWithResponse(HttpMethod method, HttpCodec codec, Class<RES> clazzRes, ThrowingFunction<HttpRequest, RES> handler) {
        return new MethodFilter(method, codec.contentType(),null, clazzRes, request -> {
            RES responseObj = handler.invoke(request);
            return createRestResponse(codec, responseObj);
        });
    }

    static <PAR, RES> HttpFilter forRestWithParamsResponse(HttpMethod method, HttpCodec codec, Class<PAR> clazzPar, Class<RES> clazzRes, ThrowingBiFunction<HttpRequest, PAR, RES> handler) {
        return new MethodFilter(method, codec.contentType(),null, clazzRes, request -> {
            PAR paramsObj = createParams(request, clazzPar);
            RES responseObj = handler.invoke(request, paramsObj);
            return createRestResponse(codec, responseObj);
        });
    }

    static <REQ, RES> HttpFilter forRestWithRequestResponse(HttpMethod method, HttpCodec codec, Class<REQ> clazzReq, Class<RES> clazzRes, ThrowingBiFunction<HttpRequest, REQ, RES> handler) {
        return new MethodFilter(method, codec.contentType(), clazzReq, clazzRes, request -> {
            REQ requestObj = request.body(codec, clazzReq);
            RES responseObj = handler.invoke(request, requestObj);
            return createRestResponse(codec, responseObj);
        });
    }

    static <PAR, REQ, RES> HttpFilter forRestWithParamsRequestResponse(HttpMethod method, HttpCodec codec, Class<PAR> clazzPar, Class<REQ> clazzReq, Class<RES> clazzRes, ThrowingTriFunction<HttpRequest, PAR, REQ, RES> handler) {
        return new MethodFilter(method, codec.contentType(), clazzReq, clazzRes, request -> {
            PAR paramsObj = createParams(request, clazzPar);
            REQ requestObj = request.body(codec, clazzReq);
            RES responseObj = handler.invoke(request, paramsObj, requestObj);
            return createRestResponse(codec, responseObj);
        });
    }

    private static <PAR> PAR createParams(HttpRequest request, Class<PAR> clazz) {
        Map<String, Object> map = new HashMap<>();
        map.putAll(request.queryParams()); // lowest prio
        map.putAll(request.formParams()); // overrides query params
        map.putAll(request.pathParams()); // overrides form params and query params
        return BINDER.createInstance(clazz, map);
    }

    private static HttpResponse createHtmlResponse(HttpRequest request, String response) {
        if(response == null) {
            return HttpResponse.empty(404);
        } else if (response.isEmpty()) {
            return HttpResponse.empty(204);
        } else if (response.startsWith("redirect=")) {
            return HttpResponse.redirect(302, request.relativizePath(response.substring(9)));
        } else {
            return HttpResponse.html(200, response);
        }
    }

    private static <RES> HttpResponse createRestResponse(HttpCodec codec, RES responseObj) {
        if(responseObj == null) {
            return HttpResponse.empty(404);
        } else if(responseObj instanceof String responseStr && responseStr.isEmpty()) {
            return HttpResponse.empty(204);
        } else {
            return HttpResponse.body(200, codec, responseObj);
        }
    }
}

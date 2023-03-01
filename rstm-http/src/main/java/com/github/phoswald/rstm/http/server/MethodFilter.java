package com.github.phoswald.rstm.http.server;

import com.github.phoswald.rstm.http.HttpMethod;
import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;

class MethodFilter implements HttpFilter {

    private final HttpMethod method;
    private final ThrowingFunction<HttpRequest, HttpResponse> handler;

    public MethodFilter(HttpMethod method, ThrowingFunction<HttpRequest, HttpResponse> handler) {
        this.method = method;
        this.handler = handler;
    }

    @Override
    public HttpResponse handle(String path, HttpRequest request) throws Exception {
        if (request.method() == method && path.isEmpty()) { // TODO (route): remaining path must be empty
            return handler.invoke(request);
        } else {
            return null;
        }
    }
}

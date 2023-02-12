package com.github.phoswald.rstm.http.server;

import java.util.List;

import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;

class CombineFilter implements HttpFilter {

    private final List<HttpFilter> filters;

    public CombineFilter(List<HttpFilter> filters) {
        this.filters = filters;
    }

    @Override
    public HttpResponse handle(String path, HttpRequest request) throws Exception {
        for (HttpFilter filter : filters) {
            HttpResponse resoponse = filter.handle(path, request);
            if (resoponse != null) {
                return resoponse;
            }
        }
        return null;
    }
}

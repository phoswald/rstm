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
    public HttpResponse handle(String path, HttpRequest request, HttpServerConfig config) throws Exception {
        for (HttpFilter filter : filters) {
            HttpResponse resoponse = filter.handle(path, request, config);
            if (resoponse != null) {
                return resoponse;
            }
        }
        return null;
    }

    @Override
    public List<RouteMetadata> createMetadata() {
        return filters.stream()
                .flatMap(filter -> filter.createMetadata().stream())
                .toList();
    }
}

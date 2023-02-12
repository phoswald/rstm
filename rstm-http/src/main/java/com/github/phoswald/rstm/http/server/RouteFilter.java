package com.github.phoswald.rstm.http.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;

class RouteFilter implements HttpFilter {

    private final List<String> routeParts;
    private final boolean routeIsDir;
    private final HttpFilter filter;

    RouteFilter(String route, HttpFilter filter) {
        this.routeParts = Arrays.asList(route.split("/"));
        this.routeIsDir = route.endsWith("/");
        this.filter = filter;
    }

    @Override
    public HttpResponse handle(String path, HttpRequest request) throws Exception {
        // TODO more hardening: review corner cases, cleanup
        List<String> pathParts = new ArrayList<>(Arrays.asList(path.replaceAll("^/", "").split("/")));
        boolean pathIsDir = path.endsWith("/");
        Map<String, String> params = new HashMap<>(request.pathParams());
        for(String routePart : routeParts) {
            if(pathParts.isEmpty()) {
                return null;
            }
            String pathPart = pathParts.get(0);
            if(routePart.startsWith("{") && routePart.endsWith("}")) {
                params.put(routePart.substring(1, routePart.length() - 1), pathPart);
            } else {
                if(!routePart.equals(pathPart)) {
                    return null;
                }
            }
            pathParts.remove(0);
        }
        path = (routeIsDir ? "" :  "/") + String.join("/", pathParts) + (pathIsDir ? "/" : "");
        if(params.size() > request.pathParams().size()) {
            request = request.toBuilder().pathParams(params).build();
        }
        return filter.handle(path, request);
    }
}

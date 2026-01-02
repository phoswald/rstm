package com.github.phoswald.rstm.http.server;

import static java.util.function.Predicate.not;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;

class RouteFilter implements HttpFilter {

    private final List<String> routeParts;
    private final boolean routeIsDir;
    private final HttpFilter filter;

    RouteFilter(String route, HttpFilter filter) {
        this.routeParts = parseParts(route);
        this.routeIsDir = isDir(route);
        this.filter = filter;
    }

    @Override
    public HttpResponse handle(String path, HttpRequest request, HttpServerConfig config) throws Exception {
        List<String> pathParts = parseParts(path);
        boolean pathIsDir = isDir(path);
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
        if(params.size() > request.pathParams().size()) {
            request = request.toBuilder().pathParams(params).build();
        }
        return filter.handle(joinParts(pathParts, pathIsDir), request, config);
    }

    private static List<String> parseParts(String path) {
        return List.of(path.split("/")).stream()
                .filter(not(String::isEmpty))
                .collect(Collectors.toList()); // cannot use toList(), must be modifiable!
    }

    private static boolean isDir(String path) {
        return path.endsWith("/");
    }

    private static String joinParts(List<String> parts, boolean isDir) {
        return String.join("/", parts) + (isDir ? "/" : "");
    }
}

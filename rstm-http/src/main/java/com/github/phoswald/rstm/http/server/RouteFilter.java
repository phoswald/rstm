package com.github.phoswald.rstm.http.server;

import static java.util.function.Predicate.not;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;

class RouteFilter implements HttpFilter {

    private final String route;
    private final List<String> routeParts;
    private final boolean routeIsDir;
    private final HttpFilter filter;

    RouteFilter(String route, HttpFilter filter) {
        this.route = route;
        this.routeParts = parseParts(route);
        this.routeIsDir = isDir(route);
        this.filter = filter;
    }

    @Override
    public HttpResponse handle(String path, HttpRequest request, HttpServerConfig config) throws Exception {
        List<String> pathParts = parseParts(path);
        boolean pathIsDir = isDir(path);
        Map<String, String> params = new HashMap<>(request.pathParams());
        for (String routePart : routeParts) {
            if (pathParts.isEmpty()) {
                return null;
            }
            String pathPart = pathParts.getFirst();
            if (isParam(routePart)) {
                params.put(getParamName(routePart), pathPart);
            } else {
                if (!routePart.equals(pathPart)) {
                    return null;
                }
            }
            pathParts.removeFirst();
        }
        if (params.size() > request.pathParams().size()) {
            request = request.toBuilder().pathParams(params).build();
        }
        return filter.handle(joinParts(pathParts, pathIsDir), request, config);
    }

    @Override
    public List<RouteMetadata> createMetadata() {
        List<String> pathParams = routeParts.stream()
                .filter(RouteFilter::isParam)
                .map(RouteFilter::getParamName)
                .toList();
        return filter.createMetadata().stream()
                .map(routeMetadata -> routeMetadata.addRoute(route, pathParams))
                .toList();
    }

    private static List<String> parseParts(String path) {
        return Stream.of(path.split("/"))
                .filter(not(String::isEmpty))
                .collect(Collectors.toList()); // cannot use toList(), must be modifiable!
    }

    private static boolean isDir(String path) {
        return path.endsWith("/");
    }

    private static boolean isParam(String routePart) {
        return routePart.startsWith("{") && routePart.endsWith("}");
    }

    private static String getParamName(String routePart) {
        return routePart.substring(1, routePart.length() - 1);
    }

    private static String joinParts(List<String> parts, boolean isDir) {
        return String.join("/", parts) + (isDir ? "/" : "");
    }
}

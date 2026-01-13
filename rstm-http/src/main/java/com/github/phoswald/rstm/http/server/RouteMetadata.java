package com.github.phoswald.rstm.http.server;

import java.util.ArrayList;
import java.util.List;

import com.github.phoswald.rstm.http.HttpMethod;

public record RouteMetadata(
        String route,
        List<String> pathParams,
        HttpMethod method,
        String contentType,
        Class<?> requestClass,
        Class<?> responseClass
) {

    public static RouteMetadata forMethod(HttpMethod method, String contentType, Class<?> requestClass, Class<?> responseClass) {
        return new RouteMetadata("", List.of(), method, contentType, requestClass, responseClass);
    }

    public RouteMetadata addRoute(String route, List<String> pathParams) {
        return new RouteMetadata(route + this.route, concat(this.pathParams, pathParams), method, contentType, requestClass, responseClass);
    }

    private static List<String> concat(List<String> list1, List<String> list2) {
        List<String> result = new ArrayList<>();
        result.addAll(list1);
        result.addAll(list2);
        return result;
    }
}

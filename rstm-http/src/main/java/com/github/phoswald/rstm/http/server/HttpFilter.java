package com.github.phoswald.rstm.http.server;

import java.util.List;

import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;

public interface HttpFilter {

    HttpResponse handle(String path, HttpRequest request, HttpServerConfig config) throws Exception;

    List<RouteMetadata> createMetadata();
}

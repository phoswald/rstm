package com.github.phoswald.rstm.http.server;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.phoswald.rstm.http.HttpMethod;
import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;
import com.sun.net.httpserver.HttpExchange;

class HttpHandler implements com.sun.net.httpserver.HttpHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final HttpServerConfig config;

    HttpHandler(HttpServerConfig config) {
        this.config = config;
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            logger.info("Handling request: URI={}", exchange.getRequestURI());
            HttpRequest request = readRequest(exchange);
            HttpResponse response = config.handler().handle(request.path(), request);
            writeResponse(exchange, response);
        } catch (Exception e) {
            logger.error("Handling request failed: URI={}", exchange.getRequestURI(), e);
        } finally {
            exchange.close();
        }
    }

    private HttpRequest readRequest(HttpExchange exchange) {
        return HttpRequest.builder() //
                .method(HttpMethod.GET) //
                .path(exchange.getRequestURI().getPath()) //
                .build();
    }

    private void writeResponse(HttpExchange exchange, HttpResponse response) throws IOException {
        if (response == null) {
            response = HttpResponse.status(404);
        }
        if (response.contentType() != null) {
            exchange.getResponseHeaders().add("content-type", response.contentType());
        }
        int responseStatus = response.status() != 0 ? response.status() : 200;
        if (response.body() != null) {
            exchange.sendResponseHeaders(responseStatus, response.body().length);
            exchange.getResponseBody().write(response.body());
        } else {
            exchange.sendResponseHeaders(responseStatus, -1 /* no response */);
        }
    }
}

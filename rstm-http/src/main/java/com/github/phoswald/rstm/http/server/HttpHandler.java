package com.github.phoswald.rstm.http.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.phoswald.rstm.http.HttpHeaderValue;
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
            logger.info("Handling {} {}", exchange.getRequestMethod(), exchange.getRequestURI());
            HttpRequest request = readRequest(exchange);
            HttpResponse response = processRequest(request);
            writeResponse(exchange, response);
        } catch (Exception e) {
            logger.error("Handling {} {} failed:", exchange.getRequestMethod(), exchange.getRequestURI(), e);
        } finally {
            exchange.close();
        }
    }

    private HttpRequest readRequest(HttpExchange exchange) throws IOException {
        Map<String,String> pathParams = new HashMap<>();
        Map<String,String> queryParams = new HashMap<>();
        Map<String,String> formParams = new HashMap<>();
        byte[] body = null;
        decodeQueryString(queryParams, exchange.getRequestURI().getQuery());
        String contentType = exchange.getRequestHeaders().getFirst("content-type");
        if (contentType != null && HttpHeaderValue.parse(contentType).valueOnly()
                .equalsIgnoreCase("application/x-www-form-urlencoded")) {
            try (var input = exchange.getRequestBody()) {
                var buffer = new ByteArrayOutputStream();
                input.transferTo(buffer);
                decodeQueryString(formParams, new String(buffer.toByteArray(), StandardCharsets.UTF_8));
            }
        } else {
            try (var input = exchange.getRequestBody()) {
                var buffer = new ByteArrayOutputStream();
                input.transferTo(buffer);
                body = buffer.toByteArray();
            }
        }
        return HttpRequest.builder() //
                .method(HttpMethod.valueOf(exchange.getRequestMethod())) //
                .path(exchange.getRequestURI().getPath()) //
                .pathParams(pathParams) //
                .queryParams(queryParams) //
                .formParams(formParams) //
                .authorization(exchange.getRequestHeaders().getFirst("authorization")) //
                .session(getSessionCookie(exchange)) //
                .body(body) //
                .build();
    }

    private void decodeQueryString(Map<String, String> queryParams, String queryString) {
        if (queryString != null) {
            // TODO: correctly handle query string encoding (see URI.getQuery() vs. URI.getRawQuery())
            for (String queryParam : queryString.split("&")) {
                int sep = queryParam.indexOf("=");
                if (sep > 0) {
                    queryParams.put( //
                            queryParam.substring(0, sep), //
                            queryParam.substring(sep + 1).replace("+", " ").replace("%20", " "));
                }
            }
        }
    }
    
    private String getSessionCookie(HttpExchange exchange) {
        String cookieList = exchange.getRequestHeaders().getFirst("cookie");
        if(cookieList != null) {
            for(String cookiePair : cookieList.split("; ")) {
                int separatorOffset = cookiePair.indexOf('=');
                if(separatorOffset != -1) {
                    String cookieName = cookiePair.substring(0, separatorOffset).trim();
                    String cookieValue = cookiePair.substring(separatorOffset + 1).trim();
                    if(cookieName.equals("session")) {
                        return cookieValue;
                    }
                }
            }
        }
        return null;
    }

    private HttpResponse processRequest(HttpRequest request) {
        try {
            return config.filter().handle(request.path(), request, config);
        } catch(Exception e) {
            logger.warn("Processing {} {} failed:", request.method(), request.path(), e);
            return HttpResponse.empty(500);
        }
    }

    private void writeResponse(HttpExchange exchange, HttpResponse response) throws IOException {
        if (response == null) {
            response = HttpResponse.empty(404);
        }
        if (response.contentType() != null) {
            exchange.getResponseHeaders().add("content-type", response.contentType());
        }
        if (response.location() != null) {
            exchange.getResponseHeaders().add("location", response.location());
        }
        if (response.session() != null) {
            exchange.getResponseHeaders().add("set-cookie", "session=" + response.session() + "; path=/; httponly; samesite=strict");
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

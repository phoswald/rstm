package com.github.phoswald.rstm.http.server;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
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
        Map<String, String> pathParams = new LinkedHashMap<>();
        Map<String, String> queryParams = new LinkedHashMap<>();
        Map<String, String> formParams = new LinkedHashMap<>();
        Map<String, String> cookies = new LinkedHashMap<>();
        byte[] body = null;
        decodeQueryString(queryParams, exchange.getRequestURI().getRawQuery());
        decodeCookies(cookies, exchange);
        String contentType = exchange.getRequestHeaders().getFirst("content-type");
        if (contentType != null && HttpHeaderValue.parse(contentType).valueOnly()
                .equalsIgnoreCase("application/x-www-form-urlencoded")) {
            try (var input = exchange.getRequestBody()) {
                var buffer = new ByteArrayOutputStream();
                input.transferTo(buffer);
                decodeQueryString(formParams, buffer.toString(UTF_8));
            }
        } else {
            try (var input = exchange.getRequestBody()) {
                var buffer = new ByteArrayOutputStream();
                input.transferTo(buffer);
                body = buffer.toByteArray();
            }
        }
        return HttpRequest.builder()
                .config(config)
                .method(HttpMethod.valueOf(exchange.getRequestMethod()))
                .path(exchange.getRequestURI().getPath())
                .pathParams(pathParams)
                .queryParams(queryParams)
                .formParams(formParams)
                .cookies(cookies)
                .authorization(exchange.getRequestHeaders().getFirst("authorization"))
                .body(body)
                .build();
    }

    private void decodeQueryString(Map<String, String> queryParams, String queryString) {
        if (queryString != null) {
            for (String queryParam : queryString.split("&")) {
                int index = queryParam.indexOf("=");
                if (index > 0) {
                    queryParams.put(queryParam.substring(0, index),
                            URLDecoder.decode(queryParam.substring(index + 1), UTF_8));
                }
            }
        }
    }

    private void decodeCookies(Map<String, String> cookies, HttpExchange exchange) {
        String cookieList = exchange.getRequestHeaders().getFirst("cookie");
        if (cookieList != null) {
            for (String cookiePair : cookieList.split("; ")) {
                int separatorOffset = cookiePair.indexOf('=');
                if (separatorOffset != -1) {
                    String cookieName = cookiePair.substring(0, separatorOffset).trim();
                    String cookieValue = cookiePair.substring(separatorOffset + 1).trim();
                    cookies.put(cookieName, cookieValue);
                }
            }
        }
    }

    private HttpResponse processRequest(HttpRequest request) {
        try {
            return config.filter().handle(request.path(), request);
        } catch (Exception e) {
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
        if (response.cookies() != null) {
            for (HttpCookie cookie : response.cookies()) {
                exchange.getResponseHeaders().add("set-cookie", cookie.toSetCookieHeaderValue());
            }
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

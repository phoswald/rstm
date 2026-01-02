package com.github.phoswald.rstm.http.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;

class ResourcesFilter implements HttpFilter {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String basePath;

    ResourcesFilter(String basePath) {
        this.basePath = Objects.requireNonNull(basePath);
    }

    @Override
    public HttpResponse handle(String path, HttpRequest request, HttpServerConfig config) throws IOException {
        if (path.contains("..")) { // TODO (security hardening): ensure path is relative
            return HttpResponse.empty(400);
        }
        if (path.endsWith("/")) {
            path = path + "index.html";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        String resource = basePath + path;
        try (InputStream input = getClass().getResourceAsStream(resource)) {
            if (input == null) {
                logger.debug("Not found: resource={}", resource);
                return null;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            input.transferTo(buffer);
            logger.debug("Sending: resource={}, size={}", resource, buffer.size());
            return HttpResponse.builder()
                    .status(200)
                    .contentType(ContentTypes.getContentType(path))
                    .body(buffer.toByteArray())
                    .build();
        }
    }
}

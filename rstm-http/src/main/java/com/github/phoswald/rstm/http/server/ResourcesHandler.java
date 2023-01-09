package com.github.phoswald.rstm.http.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;

class ResourcesHandler implements HttpFilter {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String basePath;

    ResourcesHandler(String basePath) {
        this.basePath = Objects.requireNonNull(basePath);
    }

    @Override
    public HttpResponse handle(String path, HttpRequest request) throws IOException {
        if (path.contains("..")) {
            return HttpResponse.empty(400);
        }
        String resource = basePath + path;
        try (InputStream input = getClass().getResourceAsStream(resource)) {
            if (input == null) {
                logger.warn("Not found: resource={}", resource);
                return HttpResponse.empty(404);
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            input.transferTo(buffer);
            logger.info("Sending: resource={}, size={}", resource, buffer.size());
            return HttpResponse.builder() //
                    .status(200) //
                    .contentType(ContentTypes.getContentType(path)) //
                    .body(buffer.toByteArray()) //
                    .build();
        }
    }
}

package com.github.phoswald.rstm.http.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;

class FilesystemHandler implements HttpFilter {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Path basePath;

    public FilesystemHandler(Path basePath) {
        this.basePath = Objects.requireNonNull(basePath);
    }

    @Override
    public HttpResponse handle(String path, HttpRequest request) throws IOException {
        if (path.contains("..")) {
            return HttpResponse.status(400);
        }
        Path file = basePath.resolve(path);
        if (!Files.isRegularFile(file)) {
            logger.warn("Not found: file={}", file);
            return HttpResponse.status(404);
        }
        byte[] buffer = Files.readAllBytes(file);
        logger.info("Sending: file={}, size={}", file, buffer.length);
        return HttpResponse.builder() //
                .status(200) //
                .contentType(ContentTypes.getContentType(path)) //
                .body(buffer) //
                .build();
    }
}

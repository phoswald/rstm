package com.github.phoswald.rstm.http.server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServer implements AutoCloseable {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final HttpServerConfig config;
    private final com.sun.net.httpserver.HttpServer server;

    public HttpServer(HttpServerConfig config) {
        try {
            this.config = config;
            this.server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(config.httpPort()), 0);
            this.server.createContext("/", new HttpHandler(this.config));
            this.server.start();
            logger.info("Started HTTP server: port={}", config.httpPort());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        server.stop(0);
        logger.info("Stopped HTTP server");
    }
}

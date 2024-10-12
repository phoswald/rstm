package com.github.phoswald.rstm.http.server;

/**
 * Determines MIME types for file extensions
 *
 * See https://developer.mozilla.org/en-US/docs/Web/HTTP/MIME_types/Common_types
 */
class ContentTypes {

    static String getContentType(String path) {
        if (path.endsWith(".html")) {
            return "text/html";
        } else if (path.endsWith(".ico")) {
            return "image/x-icon";
        } else if (path.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (path.endsWith(".png")) {
            return "image/png";
        } else {
            return null;
        }
    }
}

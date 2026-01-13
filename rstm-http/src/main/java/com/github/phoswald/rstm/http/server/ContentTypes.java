package com.github.phoswald.rstm.http.server;

/**
 * Determines MIME types for file extensions
 *
 * See https://developer.mozilla.org/en-US/docs/Web/HTTP/MIME_types/Common_types
 */
class ContentTypes {

    static String getContentType(String path) {
        return switch(path.substring(path.lastIndexOf(".") + 1)) {
            case "css" -> "text/css";
            case "html" -> "text/html";
            case "ico" -> "image/x-icon";
            case "jpeg" -> "image/jpeg";
            case "js" -> "text/javascript";
            case "json" -> "application/json";
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "svg" -> "image/svg+xml";
            case "txt" -> "text/plain";
            case "xhtml" -> "application/xhtml+xml";
            case "xml" -> "application/xml";
            default -> null;
        };
    }
}

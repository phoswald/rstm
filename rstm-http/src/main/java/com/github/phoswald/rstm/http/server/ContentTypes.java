package com.github.phoswald.rstm.http.server;

class ContentTypes {

    static String getContentType(String path) {
        if (path.endsWith(".html")) {
            return "text/html";
        } else if (path.endsWith(".ico")) {
            return "image/x-icon";
        } else {
            return null;
        }
    }
}

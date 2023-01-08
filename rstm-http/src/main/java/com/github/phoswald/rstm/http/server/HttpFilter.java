package com.github.phoswald.rstm.http.server;

import com.github.phoswald.rstm.http.HttpRequest;
import com.github.phoswald.rstm.http.HttpResponse;

public interface HttpFilter {

    public HttpResponse handle(String path, HttpRequest request) throws Exception;
}

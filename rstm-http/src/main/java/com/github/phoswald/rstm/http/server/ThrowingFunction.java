package com.github.phoswald.rstm.http.server;

public interface ThrowingFunction<A, B> {

    public B invoke(A request) throws Exception;
}

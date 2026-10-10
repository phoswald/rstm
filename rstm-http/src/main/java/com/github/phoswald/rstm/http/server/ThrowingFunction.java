package com.github.phoswald.rstm.http.server;

public interface ThrowingFunction<A, B> {

    B invoke(A param) throws Exception;
}

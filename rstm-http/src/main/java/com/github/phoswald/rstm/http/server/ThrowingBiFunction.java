package com.github.phoswald.rstm.http.server;

public interface ThrowingBiFunction<A, B, C> {

    public C invoke(A param1, B param2) throws Exception;
}

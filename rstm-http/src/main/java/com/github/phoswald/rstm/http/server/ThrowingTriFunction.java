package com.github.phoswald.rstm.http.server;

public interface ThrowingTriFunction<A, B, C, D> {

    D invoke(A param1, B param2, C param3) throws Exception;
}

package com.github.phoswald.rstm.http.server;

public interface ThrowingBiFunction<A, B, C> {

    public C invoke(A request1, B request2) throws Exception;
}

package com.github.phoswald.rstm.http.server;

public interface ThrowingSupplier<A> {

    public A invoke() throws Exception;
}

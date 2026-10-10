package com.github.phoswald.rstm.http.server;

public interface ThrowingSupplier<A> {

    A invoke() throws Exception;
}

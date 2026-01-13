package com.github.phoswald.rstm.http;

public interface HttpCodec {

    String contentType();

    byte[] encode(Object object);

    <T> T decode(Class<T> clazz, byte[] bytes);
}

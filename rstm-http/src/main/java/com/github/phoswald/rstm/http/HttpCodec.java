package com.github.phoswald.rstm.http;

public interface HttpCodec {

    public String getContentType();

    public byte[] encode(Object object);

    public <T> T decode(Class<T> clazz, byte[] bytes);
}

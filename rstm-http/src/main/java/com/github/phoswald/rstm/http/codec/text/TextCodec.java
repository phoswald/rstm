package com.github.phoswald.rstm.http.codec.text;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.github.phoswald.rstm.http.HttpCodec;

public class TextCodec implements HttpCodec {

    public static HttpCodec text() {
        return new TextCodec();
    }

    @Override
    public String getContentType() {
        return "text/plain";
    }

    @Override
    public byte[] encode(Object o) {
        return ((String) o).getBytes(UTF_8);
    }

    @Override
    public <T> T decode(Class<T> clazz, byte[] bytes) {
        return (T) new String(bytes, UTF_8);
    }
}

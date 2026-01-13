package com.github.phoswald.rstm.http.codec;

import static com.github.phoswald.rstm.http.HttpConstants.CONTENT_TYPE_TEXT;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.github.phoswald.rstm.http.HttpCodec;

public class TextCodec implements HttpCodec {

    public static HttpCodec text() {
        return new TextCodec();
    }

    @Override
    public String contentType() {
        return CONTENT_TYPE_TEXT;
    }

    @Override
    public byte[] encode(Object o) {
        return ((String) o).getBytes(UTF_8);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T decode(Class<T> clazz, byte[] bytes) {
        return (T) new String(bytes, UTF_8);
    }
}

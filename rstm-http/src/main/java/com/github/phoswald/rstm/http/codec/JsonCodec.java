package com.github.phoswald.rstm.http.codec;

import static com.github.phoswald.rstm.http.HttpConstants.CONTENT_TYPE_JSON;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.github.phoswald.rstm.databind.Databinder;
import com.github.phoswald.rstm.http.HttpCodec;

public class JsonCodec implements HttpCodec {

    private static final Databinder BINDER = new Databinder();

    public static HttpCodec json() {
        return new JsonCodec();
    }

    @Override
    public String contentType() {
        return CONTENT_TYPE_JSON;
    }

    @Override
    public byte[] encode(Object object) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        BINDER.toJson(object, buffer);
        return buffer.toByteArray();
    }

    @Override
    public <T> T decode(Class<T> clazz, byte[] bytes) {
        return BINDER.fromJson(new ByteArrayInputStream(bytes), clazz);
    }
}

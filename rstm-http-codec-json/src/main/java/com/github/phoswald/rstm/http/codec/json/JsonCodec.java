package com.github.phoswald.rstm.http.codec.json;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.github.phoswald.rstm.http.HttpCodec;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

public class JsonCodec implements HttpCodec {

    private static final Jsonb json = JsonbBuilder.create();

    public static HttpCodec json() {
        return new JsonCodec();
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public byte[] encode(Object object) {
        var buffer = new ByteArrayOutputStream();
        json.toJson(object, buffer);
        return buffer.toByteArray();
    }

    @Override
    public <T> T decode(Class<T> clazz, byte[] bytes) {
        return json.fromJson(new ByteArrayInputStream(bytes), clazz);
    }
}

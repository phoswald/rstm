package com.github.phoswald.rstm.http.codec.json;

import com.github.phoswald.rstm.databind.Databinder;
import com.github.phoswald.rstm.http.HttpCodec;

public class JsonCodec implements HttpCodec {

    public static HttpCodec json() {
        return new JsonCodec();
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public byte[] encode(Object object) {
        return new Databinder().toJson((Class) object.getClass(), object);
    }

    @Override
    public <T> T decode(Class<T> clazz, byte[] bytes) {
        return new Databinder().fromJson(clazz, bytes);
    }
}

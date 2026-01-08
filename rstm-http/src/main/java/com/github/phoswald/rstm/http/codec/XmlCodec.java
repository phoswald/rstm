package com.github.phoswald.rstm.http.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.github.phoswald.rstm.databind.Databinder;
import com.github.phoswald.rstm.http.HttpCodec;

public class XmlCodec implements HttpCodec {

    private static final Databinder BINDER = new Databinder();

    public static HttpCodec xml() {
        return new XmlCodec();
    }

    @Override
    public String getContentType() {
        return "application/xml";
    }

    @Override
    public byte[] encode(Object object) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        BINDER.toXml(object, buffer);
        return buffer.toByteArray();
    }

    @Override
    public <T> T decode(Class<T> clazz, byte[] bytes) {
        return BINDER.fromXml(new ByteArrayInputStream(bytes), clazz);
    }
}

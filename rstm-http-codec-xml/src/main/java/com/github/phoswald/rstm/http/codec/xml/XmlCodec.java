package com.github.phoswald.rstm.http.codec.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.github.phoswald.rstm.http.HttpCodec;

import jakarta.xml.bind.JAXB;

public class XmlCodec implements HttpCodec {

    public static HttpCodec xml() {
        return new XmlCodec();
    }

    @Override
    public String getContentType() {
        return "text/xml";
    }

    @Override
    public byte[] encode(Object object) {
        var buffer = new ByteArrayOutputStream();
        JAXB.marshal(object, buffer);
        return buffer.toByteArray();
    }

    @Override
    public <T> T decode(Class<T> clazz, byte[] bytes) {
        return JAXB.unmarshal(new ByteArrayInputStream(bytes), clazz);
    }
}

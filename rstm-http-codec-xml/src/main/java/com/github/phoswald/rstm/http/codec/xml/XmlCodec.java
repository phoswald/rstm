package com.github.phoswald.rstm.http.codec.xml;

import com.github.phoswald.rstm.databind.Databinder;
import com.github.phoswald.rstm.http.HttpCodec;

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
        return new Databinder().toXml((Class) object.getClass(), object);
    }

    @Override
    public <T> T decode(Class<T> clazz, byte[] bytes) {
        return new Databinder().fromXml(clazz, bytes);
    }
}

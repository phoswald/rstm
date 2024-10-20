package com.github.phoswald.rstm.databind;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

class XmlOutputStream implements DataOutputStream {

    private final OutputStream stream;
    private final XMLStreamWriter writer;

    XmlOutputStream(OutputStream stream) throws XMLStreamException {
        this.stream = stream;
        writer = XMLOutputFactory.newInstance().createXMLStreamWriter(stream, "UTF-8");
        writer.writeStartDocument();
        writer.writeCharacters("\n");
    }

    @Override
    public void close() throws XMLStreamException, IOException {
        writer.writeEndDocument();
        writer.close();
        stream.close();
    }

    @Override
    public void startObject(String name) throws XMLStreamException {
        writer.writeStartElement(name);
        writer.writeCharacters("\n");
    }

    @Override
    public void endObject(String name) throws XMLStreamException {
        writer.writeEndElement();
        writer.writeCharacters("\n");
    }

    @Override
    public void field(String name, Object value) throws XMLStreamException {
        if(value != null) {
            writer.writeCharacters("    ");
            writer.writeStartElement(name);
            writer.writeCharacters(value.toString());
            writer.writeEndElement();
            writer.writeCharacters("\n");
        }
    }
}

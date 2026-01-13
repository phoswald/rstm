package com.github.phoswald.rstm.databind;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

class XmlOutputStream extends DataOutputStream {

    private final String indent = "    ";
    private final boolean pretty;
    private final Writer stream;
    private final XMLStreamWriter writer;

    private int level;

    XmlOutputStream(Writer stream, boolean pretty) throws XMLStreamException {
        this.pretty = pretty;
        this.stream = stream;
        this.writer = XMLOutputFactory.newInstance().createXMLStreamWriter(stream /*, "UTF-8" */);
        writeStartDocument(); // writer.writeStartDocument("UTF-8", "1.0") does not write standalone attribute!
        if (pretty) {
            writer.writeCharacters("\n");
        }
    }

    @Override
    public void close() throws XMLStreamException, IOException {
        writer.writeEndDocument();
        writer.close();
        stream.close();
    }

    @Override
    void writeStartObject(String name) throws XMLStreamException {
        if (pretty) {
            writer.writeCharacters(indent.repeat(level));
        }
        writer.writeStartElement(name);
        if (pretty) {
            writer.writeCharacters("\n");
        }
        level++;
    }

    @Override
    void writeEndObject(String name) throws XMLStreamException {
        level--;
        if (pretty) {
            writer.writeCharacters(indent.repeat(level));
        }
        writer.writeEndElement();
        if (pretty) {
            writer.writeCharacters("\n");
        }
    }

    @Override
    void writeStartList(String name) { }

    @Override
    void writeEndList(String name) { }

    @Override
    void writeStartMap(String name) {
        throw new UnsupportedOperationException("XML does not support type Map");
    }

    @Override
    void writeEndMap(String name) { }

    @Override
    void writeValue(String name, Object value) throws XMLStreamException {
        if (pretty) {
            writer.writeCharacters(indent.repeat(level));
        }
        writer.writeStartElement(name);
        writer.writeCharacters(value.toString());
        writer.writeEndElement();
        if (pretty) {
            writer.writeCharacters("\n");
        }
    }

    private void writeStartDocument() throws XMLStreamException {
        try {
            stream.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }
}

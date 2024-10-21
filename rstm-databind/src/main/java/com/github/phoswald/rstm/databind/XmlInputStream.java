package com.github.phoswald.rstm.databind;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.github.phoswald.rstm.databind.Databinder.ClassInfo;

class XmlInputStream implements DataInputStream {

    private final InputStream stream;
    private final XMLStreamReader reader;

    XmlInputStream(InputStream stream) throws XMLStreamException {
        this.stream = stream;
        reader = XMLInputFactory.newInstance().createXMLStreamReader(stream);
    }

    @Override
    public void close() throws XMLStreamException, IOException {
        reader.close();
        stream.close();
    }

    @Override
    public <T> void readObject(ClassInfo<T> classInfo, Map<String, Object> map) throws Exception {
        startDocument();
        next();
        String objectName;
        if((objectName = startObject()) != null) {
            System.out.println("XML: objectName=" + objectName);
            next();
            String fieldName;
            while((fieldName = startObject()) != null) {
                System.out.println("XML: fieldName=" + fieldName);
//                FieldInfo<T> fieldInfo = classInfo.fields().get(fieldName);
                next();
                Object fieldValue = text();
                if(fieldValue != null) {
                    System.out.println("XML: fieldValue=" + fieldValue);
//                    if(fieldInfo.clazz() == Integer.class) {
//                        fieldValue = Integer.valueOf((String) fieldValue);
//                    }
//                    if(fieldInfo.clazz() == Long.class) {
//                        fieldValue = Long.valueOf((String) fieldValue);
//                    }
                    map.put(fieldName, fieldValue);
                }
                next();
                endObject();
                next();
            }
            endObject();
        }
        next();
        endDocument();
    }

    boolean next() throws XMLStreamException {
        while(reader.next() == XMLStreamConstants.CHARACTERS && reader.isWhiteSpace()) {

        }
        return reader.getEventType() != XMLStreamConstants.END_DOCUMENT;
    }

    boolean startDocument() {
        return reader.getEventType() == XMLStreamConstants.START_DOCUMENT;
    }

    boolean endDocument() {
        return reader.getEventType() == XMLStreamConstants.END_DOCUMENT;
    }

    String startObject() {
        if(reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
            return reader.getLocalName();
        } else {
            return null;
        }
    }

    boolean endObject() {
        if(reader.getEventType() == XMLStreamConstants.END_ELEMENT) {
            return true;
        } else {
            return false;
        }
    }

    String text() {
        if(reader.hasText()) {
            return reader.getText();
        } else {
            return null;
        }
    }

    // enum State { EOF, FIELD }
}

package com.github.phoswald.rstm.databind;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

import com.github.phoswald.rstm.databind.Databinder.ClassInfo;

class JsonInputStream implements DataInputStream {

    private final InputStream stream;
    private final JsonParser parser;

    JsonInputStream(InputStream stream) {
        this.stream = stream;
        parser = Json.createParser(stream);
    }

    @Override
    public void close() throws IOException {
        parser.close();
        stream.close();
    }

    @Override
    public <T> void readObject(ClassInfo<T> classInfo, Map<String, Object> map) throws Exception {
        Event e;
        if((e = parser.next()) == Event.START_OBJECT) {
            while((e = parser.next()) == Event.KEY_NAME) {
                String fieldName = parser.getString();
                System.out.println("Json: fieldName=" + fieldName);
                Object fieldValue = null;
                e = parser.next();
                if(e == Event.VALUE_STRING) {
                    fieldValue = parser.getString();
                }
                if(e == Event.VALUE_NUMBER) {
                    fieldValue = parser.getBigDecimal();
                }
                if(e == Event.VALUE_TRUE) {
                    fieldValue = true;
                }
                if(e == Event.VALUE_FALSE) {
                    fieldValue = false;
                }
                System.out.println("Json: fieldValue=" + fieldValue);
                map.put(fieldName, fieldValue);
            }
        }
    }
}

package com.github.phoswald.rstm.databind;

import static jakarta.json.stream.JsonGenerator.PRETTY_PRINTING;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;

class JsonOutputStream implements DataOutputStream {

    private final OutputStream stream;
    private final JsonGenerator generator;

    JsonOutputStream(OutputStream stream) throws XMLStreamException {
        this.stream = stream;
        generator = Json.createGeneratorFactory(Map.of(PRETTY_PRINTING, "true")).createGenerator(stream);
    }

    @Override
    public void close() throws IOException {
        generator.close();
        stream.write('\n'); // XXX: maybe does not work if closed (other than in memory)
        stream.close();
    }

    @Override
    public void startObject(String name) throws IOException {
        generator.writeStartObject();
    }

    @Override
    public void endObject(String name) throws IOException {
        generator.writeEnd();
    }

    @Override
    public void field(String name, Object value) throws IOException {
        if(value instanceof Number) {
            generator.write(name, new BigDecimal(value.toString()));
        } else if(value instanceof Boolean booleanValue) {
            generator.write(name, booleanValue.booleanValue());
        } else if(value != null) {
            generator.write(name, value.toString());
        }
    }
}

package com.github.phoswald.rstm.databind;

import static jakarta.json.stream.JsonGenerator.PRETTY_PRINTING;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Stack;

import javax.xml.stream.XMLStreamException;

import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;

class JsonOutputStream extends DataOutputStream {

    private final boolean pretty;
    private final Writer stream;
    private final JsonGenerator generator;
    private final Stack<Scope> stack = new Stack<>();

    JsonOutputStream(Writer stream, boolean pretty) throws XMLStreamException {
        Map<String, String> config = pretty ? Map.of(PRETTY_PRINTING, "true") : Map.of();
        this.pretty = pretty;
        this.stream = stream;
        this.generator = Json.createGeneratorFactory(config).createGenerator(stream);
        stack.push(Scope.DOCUMENT);
    }

    @Override
    public void close() throws IOException {
        generator.flush();
        if(pretty) {
            stream.write('\n');
        }
        generator.close();
    }

    @Override
    void writeStartObject(String name) throws IOException {
        if(stack.peek() == Scope.OBJECT) {
            generator.writeKey(name);
        }
        generator.writeStartObject();
        stack.push(Scope.OBJECT);
    }

    @Override
    void writeEndObject(String name) throws IOException {
        stack.pop();
        generator.writeEnd();
    }

    @Override
    void writeStartList(String name) throws Exception {
        if(stack.peek() == Scope.OBJECT) {
            generator.writeKey(name);
        }
        generator.writeStartArray();
        stack.push(Scope.LIST);
    }

    @Override
    void writeEndList(String name) throws Exception {
        stack.pop();
        generator.writeEnd();
    }

    @Override
    void writeValue(String name, Object value) throws IOException {
        if(stack.peek() == Scope.OBJECT) {
            generator.writeKey(name);
        }
        if(value instanceof Number) {
            generator.write(new BigDecimal(value.toString()));
        } else if(value instanceof Boolean booleanValue) {
            generator.write(booleanValue.booleanValue());
        } else {
            generator.write(value.toString());
        }
    }

    enum Scope {
        DOCUMENT, OBJECT, LIST
    }
}

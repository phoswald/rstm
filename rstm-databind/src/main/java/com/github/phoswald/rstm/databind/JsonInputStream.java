package com.github.phoswald.rstm.databind;

import java.io.IOException;
import java.io.Reader;

import jakarta.json.Json;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

class JsonInputStream extends DataInputStream {

    private final boolean tolerant;
    private final JsonParser parser;

    JsonInputStream(Reader stream, boolean tolerant) {
        this.tolerant = tolerant;
        this.parser = Json.createParser(stream);
    }

    @Override
    public void close() throws IOException {
        parser.close();
    }

    @Override
    Object readObject(ClassInfo classInfo) throws Exception {
        stack.push(new Scope(classInfo));
        while(parser.hasNext()) {
            Scope scope = stack.peek();
            Event event = parser.next();
            switch(event) {
                case Event.START_OBJECT:
                    if(scope.fieldInfo != null) {
                        if(scope.fieldInfo.type() instanceof RecordType recordType) {
                            scope = stack.push(new Scope(recordType.classInfo()));
                        } else if(scope.fieldInfo.type() instanceof ListType listType) {
                            if(listType.elementType() instanceof RecordType recordType) {
                                scope = stack.push(new Scope(recordType.classInfo()));
                            }
                        }
                    }
                    scope.level++;
                    break;
                case Event.END_OBJECT:
                    scope.level--;
                    if(scope.level == 0 && stack.size() > 1) {
                        Object instance = stack.pop().createInstance();
                        stack.peek().addValue(instance);
                    }
                    break;
                case Event.START_ARRAY:
                    scope.startList();
                    break;
                case Event.END_ARRAY:
                    scope.endList();
                    break;
                case Event.KEY_NAME:
                    scope.startField(parser.getString(), tolerant);
                    break;
                case Event.VALUE_STRING:
                    scope.addValue(parser.getString());
                    break;
                case Event.VALUE_NUMBER:
                    scope.addValue(parser.getBigDecimal());
                    break;
                case Event.VALUE_TRUE:
                    scope.addValue(true);
                    break;
                case Event.VALUE_FALSE:
                    scope.addValue(false);
                    break;
                case Event.VALUE_NULL:
                    break;
            }
        }
        return stack.peek().createInstance();
    }
}

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
        while (parser.hasNext()) {
            Scope scope = stack.peek();
            Event event = parser.next();
            switch (event) {
                case Event.START_OBJECT:
                    if (scope.fieldInfo != null) {
                        if (scope.fieldInfo.type() instanceof RecordType(ClassInfo classInfo1)) {
                            scope = stack.push(new Scope(classInfo1));
                        } else if (scope.fieldInfo.type() instanceof ListType(ElementType elementType)) {
                            if (elementType instanceof RecordType(ClassInfo info)) {
                                scope = stack.push(new Scope(info));
                            }
                        } else if (scope.fieldInfo.type() instanceof MapType(ElementType elementType)) {
//                          System.out.println("mapType: " + scope.fieldInfo.name());
                            if (elementType instanceof RecordType(ClassInfo info)) {
                                scope.startMap();
                                scope = stack.push(new Scope(info));
                            }
                            if (elementType instanceof SimpleType) {
                                scope.startMap();
//                              scope = stack.push(new Scope(null));
                            }
                        }
                    } else {
                        System.out.println("??? no fi");
                    }
                    scope.level++;
                    break;
                case Event.END_OBJECT:
                    scope.level--;
                    if (scope.isMap()) {
                        scope.endMap();
                    }
                    if (scope.level == 0 && stack.size() > 1) {
                        Object instance = stack.pop().createInstance();
                        System.out.println("pop " + instance);
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

package com.github.phoswald.rstm.databind;

import java.io.IOException;
import java.io.Reader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

class XmlInputStream extends DataInputStream {

    private final boolean tolerant;
    private final Reader stream;
    private final XMLStreamReader reader;

    XmlInputStream(Reader stream, boolean tolerant) throws XMLStreamException {
        this.tolerant = tolerant;
        this.stream = stream;
        this.reader = XMLInputFactory.newInstance().createXMLStreamReader(stream);
    }

    @Override
    public void close() throws XMLStreamException, IOException {
        reader.close();
        stream.close();
    }

    @Override
    Object readObject(ClassInfo classInfo) throws Exception {
        stack.push(new Scope(classInfo));
        while(reader.hasNext()) {
            Scope scope = stack.peek();
            final int event = reader.next();
            switch(event) {
                case XMLStreamConstants.START_ELEMENT:
                    if(scope.level > 0) {
                        if(scope.startField(reader.getLocalName(), tolerant)) {
                            if(scope.fieldInfo.type() instanceof RecordType recordType) {
                                scope.endList();
                                scope = stack.push(new Scope(recordType.classInfo()));
                            } else if(scope.fieldInfo.type() instanceof ListType listType) {
                                if(scope.hasFieldChanged()) {
                                    scope.startList();
                                }
                                if(listType.elementType() instanceof RecordType recordType) {
                                    scope = stack.push(new Scope(recordType.classInfo()));
                                }
                            } else {
                                scope.endList();
                            }
                        }
                    }
                    scope.level++;
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    scope.level--;
                    if(scope.level == 0 && stack.size() > 1) {
                        Object instance = stack.pop().createInstance();
                        stack.peek().addValue(instance);
                    }
                    break;
                case XMLStreamConstants.CHARACTERS:
                    if(scope.level > 1) {
                        scope.addValue(reader.getText());
                    }
                    break;
            }
        }
        return stack.peek().createInstance();
    }
}

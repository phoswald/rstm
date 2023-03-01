package com.github.phoswald.rstm.template;

import static javax.xml.stream.XMLStreamConstants.CHARACTERS;
import static javax.xml.stream.XMLStreamConstants.COMMENT;
import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

class XHtmlParser {

    private static final String NAMESPACE = "https://github.com/phoswald/rstm/rstm-template";
    private static final Pattern ATTR_PATTERN = Pattern.compile("([a-z][a-z0-9-]*)=([a-zA-Z][a-zA-Z0-9-]*)");
    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();

    private final XMLStreamReader reader;

    XHtmlParser(String input) throws XMLStreamException {
        this.reader = FACTORY.createXMLStreamReader(new StringReader(input));
    }

    HtmlDocument parseDocument(TemplateCompilation<?> compilation) throws XMLStreamException {
        List<Node> children = parseNodes(compilation);
        return new HtmlDocument(children);
    }

    private List<Node> parseNodes(TemplateCompilation<?> compilation) throws XMLStreamException {
        List<Node> nodes = new ArrayList<>();
        boolean exit = false;
        while (reader.hasNext() && !exit) {
            int xmlEvent = reader.next();
            switch (xmlEvent) {
            case START_ELEMENT:
                Map<String, String> attributes = new LinkedHashMap<>();
                Operation operation = Operation.nop(compilation);
                for (int i = 0; i < reader.getAttributeCount(); i++) {
                    String attributeNamespace = reader.getAttributeNamespace(i);
                    String attributeName = reader.getAttributeLocalName(i);
                    String attributeValue = reader.getAttributeValue(i);
                    if (Objects.equals(attributeNamespace, NAMESPACE)) {
                        operation = processAttribute(compilation, attributeName, attributeValue);
                    } else if (reader.getAttributeNamespace(i) == null) {
                        attributes.put(attributeName, attributeValue);
                    } else {
                        throw new IllegalArgumentException("Unsupported namespace: " + attributeNamespace);
                    }
                }
                List<Node> children = parseNodes(operation.nestedCompilation());
                HtmlElement element = new HtmlElement(reader.getLocalName(), attributes, children);
                nodes.add(operation.processHtmlElement(element));
                break;
            case END_ELEMENT:
                exit = true;
                break;
            case CHARACTERS:
                nodes.add(new HtmlText(reader.getText()));
                break;
            case COMMENT:
                nodes.add(new HtmlComment(reader.getText()));
                break;
            case END_DOCUMENT:
                exit = true;
                break;
            default:
                break;
            }
        }
        return nodes;
    }

    private Operation processAttribute(TemplateCompilation<?> compilation, String attributeName, String attributeValue) {
        switch (attributeName) {
            case "text":
                return Operation.text(compilation, attributeValue);
            case "attr":
                Matcher matcher = ATTR_PATTERN.matcher(attributeValue);
                if (matcher.matches()) {
                    return Operation.attr(compilation, matcher.group(1), matcher.group(2));
                } else {
                    throw new IllegalArgumentException("Invalid attribute value: " + attributeValue);
                }
            case "if":
                return Operation.iff(compilation, attributeValue);
            case "each":
                return Operation.each(compilation, attributeValue);
            default:
                throw new IllegalArgumentException("Unsupported attribute: " + attributeName);
        }
    }
}

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
import java.util.function.Function;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

class XHtmlParser {

    private static final String NAMESPACE = "https://github.com/phoswald/rstm/rstm-template";
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
                    nodes.add(processElement(compilation));
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

    private Node processElement(TemplateCompilation<?> compilation) throws XMLStreamException {
        Map<String, String> attributes = new LinkedHashMap<>();
        TemplateCompilation<?> nestedCompilation = compilation;
        List<Function<Node, Node>> transformations = new ArrayList<>();
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attributeNamespace = reader.getAttributeNamespace(i);
            String attributeName = reader.getAttributeLocalName(i);
            String attributeValue = reader.getAttributeValue(i);
            if (Objects.equals(attributeNamespace, NAMESPACE)) {
                TemplateOperation operation = processAttribute(nestedCompilation, attributeName, attributeValue);
                nestedCompilation = operation.nestedCompilation();
                transformations.add(operation.transformation());
            } else if (reader.getAttributeNamespace(i) == null) {
                attributes.put(attributeName, attributeValue);
            } else {
                throw new IllegalArgumentException("Unsupported namespace: " + attributeNamespace);
            }
        }
        List<Node> children = parseNodes(nestedCompilation);
        Node node;
        if(Objects.equals(reader.getNamespaceURI(), NAMESPACE)) {
            node = processElement(reader.getLocalName(), children) ;
        } else {
            node = new HtmlElement(reader.getLocalName(), attributes, children);
        }
        for(Function<Node, Node> transformation : transformations.reversed()) {
            node = transformation.apply(node);
        }
        return node;
    }

    private TemplateOperation processAttribute(TemplateCompilation<?> compilation, String attributeName, String attributeValue) {
        return switch (attributeName) {
            case "text" -> TemplateOperation.text(compilation, attributeValue);
            case "raw" -> TemplateOperation.raw(compilation, attributeValue);
            case "attr" -> TemplateOperation.attr(compilation, attributeValue);
            case "if" -> TemplateOperation.iff(compilation, attributeValue);
            case "each" -> TemplateOperation.each(compilation, attributeValue);
            default -> throw new IllegalArgumentException("Unsupported attribute: " + attributeName);
        };
    }

    private Node processElement(String elementName, List<Node> children) {
        return switch(elementName) {
            case "do" -> TemplateOperation.doo(children);
            default ->  throw new IllegalArgumentException("Unsupported element: " + elementName);
        };
    }
}

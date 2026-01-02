package com.github.phoswald.rstm.template;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

interface Node {

    void evaluateNode(HtmlGenerator generator, TemplateArgument<?> argument);
}

record ExprText(Property<String> property) implements Node {

    @Override
    public void evaluateNode(HtmlGenerator generator, TemplateArgument<?> argument) {
        generator.generateText(property.getValue(argument));
    }
}

record ExprAttr(Property<String> property, String attribute, HtmlElement nestedNode) implements Node {

    @Override
    public void evaluateNode(HtmlGenerator generator, TemplateArgument<?> argument) {
        String value = property.getValue(argument);
        HtmlElement node = nestedNode;
        if (value != null) {
            node = nestedNode.addAttribute(attribute, value);
        }
        node.evaluateNode(generator, argument);
    }
}

record ExprIf(Property<Boolean> property, HtmlElement nestedNode) implements Node {

    @Override
    public void evaluateNode(HtmlGenerator generator, TemplateArgument<?> argument) {
        if (property.getValue(argument).booleanValue()) {
            nestedNode.evaluateNode(generator, argument);
        }
    }
}

record ExprEach(Property<Collection<?>> property, HtmlElement nestedNode) implements Node {

    @Override
    public void evaluateNode(HtmlGenerator generator, TemplateArgument<?> argument) {
        for (Object nestedArgument : property.getValue(argument)) {
            nestedNode.evaluateNode(generator, argument.nestedArgument(nestedArgument));
        }
    }
}

record HtmlDocument(List<Node> children) implements Node {

    @Override
    public void evaluateNode(HtmlGenerator generator, TemplateArgument<?> argument) {
        generator.generateDocmentStart();
        for (Node child : children) {
            child.evaluateNode(generator, argument);
        }
        generator.generateDocmentEnd();
    }
}

record HtmlElement(String name, Map<String, String> attributes, List<Node> children) implements Node {

    @Override
    public void evaluateNode(HtmlGenerator generator, TemplateArgument<?> argument) {
        generator.generateElementStart(name, attributes);
        for (Node child : children) {
            child.evaluateNode(generator, argument);
        }
        generator.generateElementEnd(name);
    }

    HtmlElement replaceChildren(Node child) {
        return new HtmlElement(name, attributes, List.of(child));
    }

    HtmlElement addAttribute(String attributeName, String attributeValue) {
        Map<String, String> newAttributes = new LinkedHashMap<>(attributes);
        newAttributes.put(attributeName, attributeValue);
        return new HtmlElement(name, newAttributes, children);
    }
}

record HtmlText(String text) implements Node {

    @Override
    public void evaluateNode(HtmlGenerator generator, TemplateArgument<?> argument) {
        generator.generateText(text);
    }
}

record HtmlComment(String comment) implements Node {

    @Override
    public void evaluateNode(HtmlGenerator generator, TemplateArgument<?> argument) {
        generator.generateComment(comment);
    }
}

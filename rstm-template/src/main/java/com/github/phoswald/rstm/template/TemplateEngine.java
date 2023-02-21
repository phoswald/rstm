package com.github.phoswald.rstm.template;

import static java.util.Collections.singletonList;
import static javax.xml.stream.XMLStreamConstants.CHARACTERS;
import static javax.xml.stream.XMLStreamConstants.COMMENT;
import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateEngine {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    private final HtmlUtil htmlUtil = new HtmlUtil();

    public <T> Function<T, String> compile(Class<T> argumentClass, String templateName) {
        logger.info("Compiling template '{}' for {}.", templateName, argumentClass);
        String templateXHtml = loadTemplate(templateName);
        Template<T> template = parseTemplate(templateName, argumentClass, templateXHtml);
        return (argument) -> evaluateTemplate(template, argument);
    }

    private String loadTemplate(String templateName) {
        try (InputStream input = getClass().getResourceAsStream("/templates/" + templateName + ".xhtml")) {
            StringWriter buffer = new StringWriter();
            new InputStreamReader(input, StandardCharsets.UTF_8).transferTo(buffer);
            return buffer.toString();

        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load template " + templateName, e);
        }
    }

    private <T> Template<T> parseTemplate(String templateName, Class<T> argumentClass, String templateXHtml) {
        try {
            XMLStreamReader reader = xmlFactory.createXMLStreamReader(new StringReader(templateXHtml));
            HtmlDocument documentNode = parseDocument(reader, argumentClass);
            return new Template<>(templateName, argumentClass, documentNode);

        } catch (XMLStreamException e) {
            throw new IllegalArgumentException("Failed to parse XML.", e);
        }
    }

    private HtmlDocument parseDocument(XMLStreamReader reader, Class<?> argumentClass) throws XMLStreamException {
        List<AnyNode> children = parseNodes(reader, argumentClass);
        return new HtmlDocument(children);
    }

    private List<AnyNode> parseNodes(XMLStreamReader reader, Class<?> argumentClass) throws XMLStreamException {
        List<AnyNode> nodes = new ArrayList<>();
        boolean exit = false;
        while (reader.hasNext() && !exit) {
            int xmlEvent = reader.next();
            switch (xmlEvent) {
                case START_ELEMENT:
                    Map<String, String> attributes = new LinkedHashMap<>();
                    Class<?> nestedArgumentClass = argumentClass;
                    Function<HtmlElement, AnyNode> postProcessing = htmlElement -> htmlElement;
                    for(int i = 0; i < reader.getAttributeCount(); i++) {
                        if(Objects.equals(reader.getAttributeNamespace(i), "https://github.com/phoswald/rstm/rstm-template")) {
                            if(Objects.equals(reader.getAttributeLocalName(i), "text")) {
                                Property property = lookupProperty(argumentClass, reader.getAttributeValue(i));
                                logger.trace("ExprText: {}::{}", argumentClass.getName(), property.name());
                                postProcessing = htmlElement -> htmlElement.replaceChildren(new ExprText(property));

                            } else if(Objects.equals(reader.getAttributeLocalName(i), "attr")) {
                                Pattern p = Pattern.compile("([a-z][a-z0-9-]*)=([a-z][a-z0-9-]*)");
                                Matcher m = p.matcher(reader.getAttributeValue(i));
                                if(m.matches()) {
                                    Property property = lookupProperty(argumentClass, m.group(2));
                                    logger.trace("ExprAttr: {}::{}", argumentClass.getName(), property.name());
                                    postProcessing = htmlElement -> new ExprAttr(property, m.group(1), htmlElement);
                                } else {
                                    throw new IllegalArgumentException("Invalid attribute value: " + reader.getAttributeValue(i));
                                }

                            } else if(Objects.equals(reader.getAttributeLocalName(i), "if")) {
                                Property property = lookupProperty(argumentClass, reader.getAttributeValue(i));
                                logger.trace("ExprIf:   {}::{}", argumentClass.getName(), property.name());
                                postProcessing = htmlElement -> new ExprIf(property, htmlElement);

                            } else if(Objects.equals(reader.getAttributeLocalName(i), "each")) {
                                Property property = lookupProperty(argumentClass, reader.getAttributeValue(i));
                                Function<Object, Collection<?>> accessor;
                                if(property.type() instanceof Class<?> typeClass
                                        && typeClass.isArray()) {
                                    nestedArgumentClass = typeClass.getComponentType();
                                    accessor = argument -> Arrays.asList(isNull((Object[]) property.accessor().apply(argument), new Object[0]));
                                    logger.trace("ExprEach: Array of {}", nestedArgumentClass);
                                } else if(property.type() instanceof ParameterizedType paramType
                                        && paramType.getRawType() instanceof Class<?> typeClass
                                        && Collection.class.isAssignableFrom(typeClass)
                                        && paramType.getActualTypeArguments().length == 1
                                        && paramType.getActualTypeArguments()[0] instanceof Class<?> collectionArgumentClass) {
                                    logger.trace("ExprEach: Collection of {}", collectionArgumentClass);
                                    nestedArgumentClass = collectionArgumentClass;
                                    accessor = argument -> isNull((Collection<?>) property.accessor.apply(argument), Collections.emptyList());
                                } else {
                                    throw new IllegalArgumentException("Invalid type: " + property.type());
                                }
                                logger.trace("ExprEach: {}::{} -> {}", argumentClass.getName(), property.name(), nestedArgumentClass.getName());
                                postProcessing = htmlElement -> new ExprEach(accessor, htmlElement);

                            } else {
                                throw new IllegalArgumentException("Unsupported attribute: " + reader.getAttributeLocalName(i));
                            }
                        } else if(reader.getAttributeNamespace(i) == null) {
                            attributes.put(reader.getAttributeLocalName(i), reader.getAttributeValue(i));

                        } else {
                            throw new IllegalArgumentException("Unsupported namespace: " + reader.getAttributeNamespace(i));
                        }
                    }
                    List<AnyNode> children = parseNodes(reader, nestedArgumentClass);
                    HtmlElement element = new HtmlElement(reader.getLocalName(), attributes, children);
                    nodes.add(postProcessing.apply(element));
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
                    logger.warn("Unknown XML event {}", xmlEvent);
            }
        }
        return nodes;
    }

    private Property lookupProperty(Class<?> clazz, String name) {
        try {
            Method method = clazz.getMethod(name);
            Function<Object, Object> accessor = instance -> invokeAccessor(clazz, name, method, instance);
            return new Property(name, method.getGenericReturnType(), accessor);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalArgumentException("Failed to lookup method: " + clazz.getName() + "::" + name, e);
        }
    }

    private Object invokeAccessor(Class<?> clazz, String name, Method accessor, Object instance) {
        try {
            if(instance == null) {
                return null;
            } else {
                return accessor.invoke(instance);
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to invoke method: " + clazz.getName() + "::" + name + ", instance of " + instance.getClass().getName(), e);
        }
    }

    private <T> String evaluateTemplate(Template<T> template, T argument) {
        logger.debug("Evaluating template '{}' for {}.", template.name(), template.argumentClass());

        StringBuilder buffer = new StringBuilder();
        evaluateNode(buffer, template.documentNode(), argument);

        String result = buffer.toString();
        logger.debug("Generated HTML output:\n{}", result);
        return result;
    }

    private void evaluateNodes(StringBuilder buffer, List<AnyNode> nodes, Object argument) {
        for(AnyNode node : nodes) {
            evaluateNode(buffer, node, argument);
        }
    }

    private void evaluateNode(StringBuilder buffer, AnyNode node, Object argument) {
        if(node instanceof ExprNode exprNode) {
            if(exprNode instanceof ExprText exprText) {
                htmlUtil.generateText(buffer, exprText.lookup(argument));

            } else if(exprNode instanceof ExprAttr exprAttr) {
                String value = exprAttr.lookup(argument);
                evaluateNode(buffer, exprAttr.nestedNode().addAttribute(exprAttr.attribute(), value), argument);

            } else if(exprNode instanceof ExprIf exprIf) {
                boolean condition = exprIf.lookup(argument);
                if(condition) {
                    evaluateNode(buffer, exprIf.nestedNode(), argument);
                }

            } else if(exprNode instanceof ExprEach exprEach) {
                Collection<?> nestedArguments = exprEach.lookup(argument);
                for(Object nestedArgument : nestedArguments) {
                    evaluateNode(buffer, exprEach.nestedNode(), nestedArgument);
                }
            }

        } else if(node instanceof HtmlNode htmlNode) {
            if(htmlNode instanceof HtmlDocument htmlDocument) {
                htmlUtil.generateDocmentStart(buffer);
                evaluateNodes(buffer, htmlDocument.children(), argument);
                htmlUtil.generateDocmentEnd(buffer);

            } else if(htmlNode instanceof HtmlElement htmlElement) {
                htmlUtil.generateElementStart(buffer, htmlElement.name(), htmlElement.attributes());
                evaluateNodes(buffer, htmlElement.children(), argument);
                htmlUtil.generateElementEnd(buffer, htmlElement.name());

            } else if(htmlNode instanceof HtmlText htmlText) {
                htmlUtil.generateText(buffer, htmlText.text());

            } else if(htmlNode instanceof HtmlComment htmlComment) {
                htmlUtil.generateComment(buffer, htmlComment.comment());
            }
        }
    }

    private static <T> T isNull(T obj, T dflt) {
        return obj != null ? obj : dflt;
    }

    record Property(String name, Type type, Function<Object, Object> accessor) { }

    record Template<T>( //
            String name, //
            Class<T> argumentClass, //
            HtmlDocument documentNode //
    ) { }

    sealed interface AnyNode permits ExprNode, HtmlNode { }

    sealed interface ExprNode extends AnyNode permits ExprText, ExprAttr, ExprIf, ExprEach { }

    record ExprText( //
            Property property //
    ) implements ExprNode {

        String lookup(Object argument) {
            Object result = property.accessor().apply(argument);
            return result == null ? null : result.toString();
        }
    }

    record ExprAttr( //
            Property property, //
            String attribute, //
            HtmlElement nestedNode //
    ) implements ExprNode {

        String lookup(Object argument) {
            Object result = property.accessor().apply(argument);
            return result == null ? null : result.toString();
        }
    }

    record ExprIf( //
            Property property, //
            HtmlElement nestedNode //
    ) implements ExprNode {

        boolean lookup(Object argument) {
            Object result = property.accessor().apply(argument);
            return result != null && result != Boolean.FALSE && !result.toString().isEmpty();
        }
    }

    record ExprEach( //
            Function<Object, Collection<?>> accessor, //
            HtmlElement nestedNode //
    ) implements ExprNode {

        Collection<?> lookup(Object argument) {
            return accessor.apply(argument);
        }
    }

    sealed interface HtmlNode extends AnyNode permits HtmlDocument, HtmlElement, HtmlText, HtmlComment { }

    record HtmlDocument( //
            List<AnyNode> children //
    ) implements HtmlNode { }

    record HtmlElement( //
            String name, //
            Map<String, String> attributes, //
            List<AnyNode> children //
    ) implements HtmlNode {

        HtmlElement replaceChildren(AnyNode child) {
            return new HtmlElement(name, attributes, singletonList(child));
        }

        HtmlElement addAttribute(String name, String value) {
            Map<String, String> newAttributes = new LinkedHashMap<>(attributes);
            newAttributes.put(name, value);
            return new HtmlElement(name, newAttributes, children);
        }
    }

    record HtmlText( //
            String text //
    ) implements HtmlNode { }

    record HtmlComment( //
            String comment //
    ) implements HtmlNode { }
}

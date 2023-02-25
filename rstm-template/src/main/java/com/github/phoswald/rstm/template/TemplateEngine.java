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
import java.io.UncheckedIOException;
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
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.function.BiFunction;
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

    public <T> Template<T> compile(Class<T> argumentClass, String templateName) {
        logger.info("Compiling template '{}' for {}.", templateName, argumentClass);
        String templateXHtml = loadTemplate(templateName);
        ResourceBundle templateResources = loadResourceBundle(templateName, Locale.ROOT);
        TemplateNode<T> template = parseTemplate(argumentClass, templateName, templateXHtml, templateResources);
        return (argument, locale) -> evaluateTemplate(template, argument, loadResourceBundle(templateName, locale));
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

    private ResourceBundle loadResourceBundle(String templateName, Locale locale) {
        try {
            return ResourceBundle.getBundle("templates." + templateName, locale);
        } catch(MissingResourceException e) {
            try {
                return new PropertyResourceBundle(new StringReader(""));
            } catch (IOException e2) {
                throw new UncheckedIOException(e2);
            }
        }
    }

    private <T> TemplateNode<T> parseTemplate(Class<T> argumentClass, String templateName, String templateXHtml, ResourceBundle templateResources) {
        try {
            XMLStreamReader templateReader = xmlFactory.createXMLStreamReader(new StringReader(templateXHtml));
            HtmlDocument documentNode = parseDocument(argumentClass, templateReader, templateResources);
            return new TemplateNode<>(templateName, argumentClass, documentNode);

        } catch (XMLStreamException e) {
            throw new IllegalArgumentException("Failed to parse XML.", e);
        }
    }

    private HtmlDocument parseDocument(Class<?> argumentClass, XMLStreamReader templateReader, ResourceBundle templateResources) throws XMLStreamException {
        List<AnyNode> children = parseNodes(argumentClass, templateReader, templateResources);
        return new HtmlDocument(children);
    }

    private List<AnyNode> parseNodes(Class<?> argumentClass, XMLStreamReader templateReader, ResourceBundle templateResources) throws XMLStreamException {
        List<AnyNode> nodes = new ArrayList<>();
        boolean exit = false;
        while (templateReader.hasNext() && !exit) {
            int xmlEvent = templateReader.next();
            switch (xmlEvent) {
                case START_ELEMENT:
                    Map<String, String> attributes = new LinkedHashMap<>();
                    Class<?> nestedArgumentClass = argumentClass;
                    Function<HtmlElement, AnyNode> postProcessing = htmlElement -> htmlElement;
                    for(int i = 0; i < templateReader.getAttributeCount(); i++) {
                        if(Objects.equals(templateReader.getAttributeNamespace(i), "https://github.com/phoswald/rstm/rstm-template")) {
                            if(Objects.equals(templateReader.getAttributeLocalName(i), "text")) {
                                Property property = lookupProperty(argumentClass, templateResources, templateReader.getAttributeValue(i));
                                logger.trace("ExprText: {}::{}", argumentClass.getName(), property.name());
                                postProcessing = htmlElement -> htmlElement.replaceChildren(new ExprText(property));

                            } else if(Objects.equals(templateReader.getAttributeLocalName(i), "attr")) {
                                Pattern p = Pattern.compile("([a-z][a-z0-9-]*)=([a-z][a-z0-9-]*)");
                                Matcher m = p.matcher(templateReader.getAttributeValue(i));
                                if(m.matches()) {
                                    Property property = lookupProperty(argumentClass, templateResources, m.group(2));
                                    logger.trace("ExprAttr: {}::{}", argumentClass.getName(), property.name());
                                    postProcessing = htmlElement -> new ExprAttr(property, m.group(1), htmlElement);
                                } else {
                                    throw new IllegalArgumentException("Invalid attribute value: " + templateReader.getAttributeValue(i));
                                }

                            } else if(Objects.equals(templateReader.getAttributeLocalName(i), "if")) {
                                Property property = lookupProperty(argumentClass, templateResources, templateReader.getAttributeValue(i));
                                logger.trace("ExprIf:   {}::{}", argumentClass.getName(), property.name());
                                postProcessing = htmlElement -> new ExprIf(property, htmlElement);

                            } else if(Objects.equals(templateReader.getAttributeLocalName(i), "each")) {
                                Property property = lookupProperty(argumentClass, templateResources, templateReader.getAttributeValue(i));
                                BiFunction<Object, ResourceBundle, Collection<?>> accessor;
                                if(property.type() instanceof Class<?> typeClass
                                        && typeClass.isArray()) {
                                    nestedArgumentClass = typeClass.getComponentType();
                                    accessor = (argument, resources) -> Arrays.asList(isNull((Object[]) property.accessor().apply(argument, resources), new Object[0]));
                                    logger.trace("ExprEach: Array of {}", nestedArgumentClass);
                                } else if(property.type() instanceof ParameterizedType paramType
                                        && paramType.getRawType() instanceof Class<?> typeClass
                                        && Collection.class.isAssignableFrom(typeClass)
                                        && paramType.getActualTypeArguments().length == 1
                                        && paramType.getActualTypeArguments()[0] instanceof Class<?> collectionArgumentClass) {
                                    logger.trace("ExprEach: Collection of {}", collectionArgumentClass);
                                    nestedArgumentClass = collectionArgumentClass;
                                    accessor = (argument, resources) -> isNull((Collection<?>) property.accessor.apply(argument, resources), Collections.emptyList());
                                } else if(property.type() instanceof ParameterizedType paramType
                                        && paramType.getRawType() instanceof Class<?> typeClass
                                        && Map.class.isAssignableFrom(typeClass)) {
                                    logger.trace("ExprEach: Map");
                                    nestedArgumentClass = Map.Entry.class;
                                    accessor = (argument, resources) -> isNull((Map<?,?>) property.accessor.apply(argument, resources), Collections.emptyMap()).entrySet();
                                } else {
                                    throw new IllegalArgumentException("Invalid type: " + property.type());
                                }
                                logger.trace("ExprEach: {}::{} -> {}", argumentClass.getName(), property.name(), nestedArgumentClass.getName());
                                postProcessing = htmlElement -> new ExprEach(accessor, htmlElement);

                            } else {
                                throw new IllegalArgumentException("Unsupported attribute: " + templateReader.getAttributeLocalName(i));
                            }
                        } else if(templateReader.getAttributeNamespace(i) == null) {
                            attributes.put(templateReader.getAttributeLocalName(i), templateReader.getAttributeValue(i));

                        } else {
                            throw new IllegalArgumentException("Unsupported namespace: " + templateReader.getAttributeNamespace(i));
                        }
                    }
                    List<AnyNode> children = parseNodes(nestedArgumentClass, templateReader, templateResources);
                    HtmlElement element = new HtmlElement(templateReader.getLocalName(), attributes, children);
                    nodes.add(postProcessing.apply(element));
                    break;
                case END_ELEMENT:
                    exit = true;
                    break;
                case CHARACTERS:
                    nodes.add(new HtmlText(templateReader.getText()));
                    break;
                case COMMENT:
                    nodes.add(new HtmlComment(templateReader.getText()));
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

    private Property lookupProperty(Class<?> clazz, ResourceBundle templateResources, String name) {
        if(name.startsWith("#")) {
            templateResources.getString(name.substring(1)); // check, throws MissingResourceException
            BiFunction<Object, ResourceBundle, Object> accessor = (instance, resources) -> getResourceText(resources, name.substring(1));
            return new Property(name, String.class, accessor);
        }
        try {
            Method method = clazz.getMethod(name);
            BiFunction<Object, ResourceBundle, Object> accessor = (instance, resources) -> invokeAccessor(clazz, name, method, instance);
            return new Property(name, method.getGenericReturnType(), accessor);
        } catch (NoSuchMethodException | SecurityException e) {
            try {
                Method method = clazz.getMethod("get" + name.substring(0, 1).toUpperCase() + name.substring(1));
                BiFunction<Object, ResourceBundle, Object> accessor = (instance, resources) -> invokeAccessor(clazz, name, method, instance);
                return new Property(name, method.getGenericReturnType(), accessor);
            } catch (NoSuchMethodException | SecurityException e2) {
                throw new IllegalArgumentException("Failed to lookup method: " + clazz.getName() + "::" + name, e);
            }
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

    private String getResourceText(ResourceBundle templateResources, String name) {
        return templateResources.getString(name);
    }

    private <T> String evaluateTemplate(TemplateNode<T> template, T argument, ResourceBundle resources) {
        logger.debug("Evaluating template '{}' for {} and {}.", template.name(), template.argumentClass(), resources);

        StringBuilder buffer = new StringBuilder();
        evaluateNode(buffer, template.documentNode(), argument, resources);

        String result = buffer.toString();
        logger.debug("Generated HTML output:\n{}", result);
        return result;
    }

    private void evaluateNodes(StringBuilder buffer, List<AnyNode> nodes, Object argument, ResourceBundle resources) {
        for(AnyNode node : nodes) {
            evaluateNode(buffer, node, argument, resources);
        }
    }

    private void evaluateNode(StringBuilder buffer, AnyNode node, Object argument, ResourceBundle resources) {
        if(node instanceof ExprNode exprNode) {
            if(exprNode instanceof ExprText exprText) {
                htmlUtil.generateText(buffer, exprText.lookup(argument, resources));

            } else if(exprNode instanceof ExprAttr exprAttr) {
                String value = exprAttr.lookup(argument, resources);
                evaluateNode(buffer, exprAttr.nestedNode().addAttribute(exprAttr.attribute(), value), argument, resources);

            } else if(exprNode instanceof ExprIf exprIf) {
                boolean condition = exprIf.lookup(argument, resources);
                if(condition) {
                    evaluateNode(buffer, exprIf.nestedNode(), argument, resources);
                }

            } else if(exprNode instanceof ExprEach exprEach) {
                Collection<?> nestedArguments = exprEach.lookup(argument, resources);
                for(Object nestedArgument : nestedArguments) {
                    evaluateNode(buffer, exprEach.nestedNode(), nestedArgument, resources);
                }
            }

        } else if(node instanceof HtmlNode htmlNode) {
            if(htmlNode instanceof HtmlDocument htmlDocument) {
                htmlUtil.generateDocmentStart(buffer);
                evaluateNodes(buffer, htmlDocument.children(), argument, resources);
                htmlUtil.generateDocmentEnd(buffer);

            } else if(htmlNode instanceof HtmlElement htmlElement) {
                htmlUtil.generateElementStart(buffer, htmlElement.name(), htmlElement.attributes());
                evaluateNodes(buffer, htmlElement.children(), argument, resources);
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

    record Property(String name, Type type, BiFunction<Object, ResourceBundle, Object> accessor) { }

    record TemplateNode<T>( //
            String name, //
            Class<T> argumentClass, //
            HtmlDocument documentNode //
    ) { }

    sealed interface AnyNode permits ExprNode, HtmlNode { }

    sealed interface ExprNode extends AnyNode permits ExprText, ExprAttr, ExprIf, ExprEach { }

    record ExprText( //
            Property property //
    ) implements ExprNode {

        String lookup(Object argument, ResourceBundle resources) {
            Object result = property.accessor().apply(argument, resources);
            return result == null ? null : result.toString();
        }
    }

    record ExprAttr( //
            Property property, //
            String attribute, //
            HtmlElement nestedNode //
    ) implements ExprNode {

        String lookup(Object argument, ResourceBundle resources) {
            Object result = property.accessor().apply(argument, resources);
            return result == null ? null : result.toString();
        }
    }

    record ExprIf( //
            Property property, //
            HtmlElement nestedNode //
    ) implements ExprNode {

        boolean lookup(Object argument, ResourceBundle resources) {
            Object result = property.accessor().apply(argument, resources);
            return result != null && result != Boolean.FALSE && !result.toString().isEmpty();
        }
    }

    record ExprEach( //
            BiFunction<Object, ResourceBundle, Collection<?>> accessor, //
            HtmlElement nestedNode //
    ) implements ExprNode {

        Collection<?> lookup(Object argument, ResourceBundle resources) {
            return accessor.apply(argument, resources);
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

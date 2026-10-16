package com.github.phoswald.rstm.template;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record TemplateOperation(TemplateCompilation<?> nestedCompilation, Function<Node, Node> transformation) {

    private static final Pattern ATTR_PATTERN = Pattern.compile("([a-z][a-z0-9-]*)=([a-zA-Z][a-zA-Z0-9-]*)");

    static TemplateOperation text(TemplateCompilation<?> compilation, String propertyName) {
        Property<?> property = Property.create(compilation, propertyName);
        return new TemplateOperation(compilation, node -> replaceChildren(node, new ExprText(property.toStringProperty())));
    }

    static TemplateOperation raw(TemplateCompilation<?> compilation, String propertyName) {
        Property<?> property = Property.create(compilation, propertyName);
        return new TemplateOperation(compilation, node -> replaceChildren(node, new ExprTextRaw(property.toStringProperty())));
    }

    static TemplateOperation attr(TemplateCompilation<?> compilation, String attributeAssignments) {
        List<ExprAttr.Attribute> attributes = new ArrayList<>();
        for(String attributeAssignment : attributeAssignments.split(";")) {
            if(attributeAssignment.isEmpty()) {
                continue;
            }
            Matcher matcher = ATTR_PATTERN.matcher(attributeAssignment);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid attribute assignment: " + attributeAssignment);
            }
            String attributeName = matcher.group(1);
            String propertyName = matcher.group(2);
            Property<?> property = Property.create(compilation, propertyName);
            attributes.add(new ExprAttr.Attribute(attributeName, property.toStringProperty()));
        }
        return new TemplateOperation(compilation, node -> new ExprAttr(attributes, requireElement(node)));
    }

    static TemplateOperation iff(TemplateCompilation<?> compilation, String propertyName) {
        Property<?> property = Property.create(compilation, propertyName);
        return new TemplateOperation(compilation, node -> new ExprIf(property.toBooleanProperty(), node));
    }

    static TemplateOperation each(TemplateCompilation<?> compilation, String propertyName) {
        Property<?> property = Property.create(compilation, propertyName);
        if (property.type() instanceof Class<?> typeClass
                && typeClass.isArray()) {
            return new TemplateOperation(
                    compilation.nestedCompilation(typeClass.getComponentType()),
                    node -> new ExprEach(property.toArrayProperty(), node));
        } else if (property.type() instanceof ParameterizedType paramType
                && paramType.getRawType() instanceof Class<?> typeClass
                && Collection.class.isAssignableFrom(typeClass)
                && paramType.getActualTypeArguments().length == 1
                && paramType.getActualTypeArguments()[0] instanceof Class<?> argumentClassColl) {
            return new TemplateOperation(
                    compilation.nestedCompilation(argumentClassColl),
                    node -> new ExprEach(property.toCollectionProperty(), node));
        } else if (property.type() instanceof ParameterizedType paramType
                && paramType.getRawType() instanceof Class<?> typeClass
                && Map.class.isAssignableFrom(typeClass)) {
            return new TemplateOperation(
                    compilation.nestedCompilation(Map.Entry.class),
                    node -> new ExprEach(property.toMapProperty(), node));
        } else {
            throw new IllegalArgumentException("Invalid type: " + property.type());
        }
    }

    static Node doo(List<Node> children) {
        return new ExprDo(children);
    }

    private static HtmlElement requireElement(Node node) {
        if(node instanceof HtmlElement element) {
            return element;
        } else {
            throw new UnsupportedOperationException("HtmlElement expected");
        }
    }

    private static Node replaceChildren(Node node, Node child) {
        if(node instanceof  HtmlElement element) {
            return element.replaceChildren(child);
        } else {
            return child;
        }
    }
}

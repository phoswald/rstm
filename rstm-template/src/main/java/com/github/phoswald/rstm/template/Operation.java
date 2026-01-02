package com.github.phoswald.rstm.template;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

record Operation(TemplateCompilation<?> nestedCompilation, Function<HtmlElement, Node> postProcessing) {

    Node processHtmlElement(HtmlElement htmlElement) {
        return postProcessing.apply(htmlElement);
    }

    static Operation nop(TemplateCompilation<?> compilation) {
        return new Operation(compilation, htmlElement -> htmlElement);
    }

    static Operation text(TemplateCompilation<?> compilation, String propertyName) {
        Property<?> property = Property.create(compilation, propertyName);
        return new Operation(compilation,
                htmlElement -> htmlElement.replaceChildren(new ExprText(property.toStringProperty())));
    }

    static Operation attr(TemplateCompilation<?> compilation, String attributeName, String propertyName) {
        Property<?> property = Property.create(compilation, propertyName);
        return new Operation(compilation,
                htmlElement -> new ExprAttr(property.toStringProperty(), attributeName, htmlElement));
    }

    static Operation iff(TemplateCompilation<?> compilation, String propertyName) {
        Property<?> property = Property.create(compilation, propertyName);
        return new Operation(compilation,
                htmlElement -> new ExprIf(property.toBooleanProperty(), htmlElement));
    }

    static Operation each(TemplateCompilation<?> compilation, String propertyName) {
        Property<?> property = Property.create(compilation, propertyName);
        if (property.type() instanceof Class<?> typeClass
                && typeClass.isArray()) {
            return new Operation(
                    compilation.nestedCompilation(typeClass.getComponentType()),
                    htmlElement -> new ExprEach(property.toArrayProperty(), htmlElement));
        } else if (property.type() instanceof ParameterizedType paramType
                && paramType.getRawType() instanceof Class<?> typeClass
                && Collection.class.isAssignableFrom(typeClass)
                && paramType.getActualTypeArguments().length == 1
                && paramType.getActualTypeArguments()[0] instanceof Class<?> argumentClassColl) {
            return new Operation(
                    compilation.nestedCompilation(argumentClassColl),
                    htmlElement -> new ExprEach(property.toCollectionProperty(), htmlElement));
        } else if (property.type() instanceof ParameterizedType paramType
                && paramType.getRawType() instanceof Class<?> typeClass
                && Map.class.isAssignableFrom(typeClass)) {
            return new Operation(
                    compilation.nestedCompilation(Map.Entry.class),
                    htmlElement -> new ExprEach(property.toMapProperty(), htmlElement));
        } else {
            throw new IllegalArgumentException("Invalid type: " + property.type());
        }
    }
}

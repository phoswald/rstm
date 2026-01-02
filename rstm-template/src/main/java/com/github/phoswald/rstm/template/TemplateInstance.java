package com.github.phoswald.rstm.template;

import java.util.Locale;
import java.util.ResourceBundle;

record TemplateInstance<T>(
        TemplateEngine engine,
        String name,
        Class<T> argumentClass,
        HtmlDocument documentNode
) implements Template<T> {

    @Override
    public String evaluate(T argument, Locale locale) {
        ResourceBundle resources = engine.findResourceBundle(name, locale);
        return engine.evaluateTemplate(this, new TemplateArgument<T>(argument, resources));
    }
}

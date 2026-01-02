package com.github.phoswald.rstm.template;

import java.util.Locale;
import java.util.ResourceBundle;

record TemplateArgument<T>(T instance, ResourceBundle resources) {

    Locale locale() {
        return resources.getLocale();
    }

    <T2> TemplateArgument<T2> nestedArgument(T2 nestedInstance) {
        return new TemplateArgument<T2>(nestedInstance, resources);
    }
}

package com.github.phoswald.rstm.template;

import java.util.ResourceBundle;

record TemplateCompilation<T>(Class<T> argumentClass, ResourceBundle resources) {

    <T2> TemplateCompilation<T2> nestedCompilation(Class<T2> nestedArgumentClass) {
        return new TemplateCompilation<T2>(nestedArgumentClass, resources);
    }
}

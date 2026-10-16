package com.github.phoswald.rstm.template;

import java.util.Locale;

public interface Template<T> {

    default String evaluate(T argument) {
        return evaluate(argument, Locale.getDefault());
    }

    String evaluate(T argument, Locale locale);
}

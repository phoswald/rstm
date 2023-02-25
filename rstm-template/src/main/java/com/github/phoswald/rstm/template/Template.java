package com.github.phoswald.rstm.template;

import java.util.Locale;

public interface Template<T> {

    public default String evaluate(T argument) {
        return evaluate(argument, Locale.getDefault());
    }

    public String evaluate(T argument, Locale locale);
}

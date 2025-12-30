package com.github.phoswald.rstm.http.metrics;

import java.util.Comparator;

public record MetricLabel(String name, String value) {

    static final Comparator<MetricLabel> COMPARE = Comparator
            .comparing(MetricLabel::name)
            .thenComparing(MetricLabel::value);

    void format(StringBuilder builder) {
        builder.append(name);
        builder.append('=');
        builder.append('"');
        int len = value.length();
        for(int pos = 0; pos < len; pos++) {
            char c = value.charAt(pos);
            if(c == '\\' || c == '"' || c == '\n') {
                builder.append('\\');
                builder.append(c == '\n' ? 'n' : c);
            } else {
                builder.append(c);
            }
        }
        builder.append('"');
    }
}

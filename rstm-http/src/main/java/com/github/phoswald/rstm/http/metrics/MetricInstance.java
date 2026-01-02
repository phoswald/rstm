package com.github.phoswald.rstm.http.metrics;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public record MetricInstance(String name, List<MetricLabel> labels) {

    private static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    static final Comparator<MetricInstance> COMPARE = Comparator
            .comparing(MetricInstance::name)
            .thenComparing(MetricInstance::labels, new ListComparator<>(MetricLabel.COMPARE));

    static MetricInstance create(String name, MetricLabel... labels) {
        if (name == null || !NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid metric name: " + name);
        }
        Map<String, Object> names = new HashMap<>();
        for (var label : labels) {
            if (label.name() == null || !NAME_PATTERN.matcher(label.name()).matches()) {
                throw new IllegalArgumentException("Invalid label name: " + label.name());
            }
            if (label.value() == null) {
                throw new IllegalArgumentException("Invalid label value");
            }
            if (names.putIfAbsent(label.name(), label) != null) {
                throw new IllegalArgumentException("Duplicate label name: " + label.name());
            }
        }
        return new MetricInstance(name, Stream.of(labels).sorted(MetricLabel.COMPARE).toList());
    }

    void formatValue(StringBuilder builder, double doubleValue) {
        format(builder);
        builder.append(' ');
        long longValue = (long) doubleValue;
        if ((double) longValue == doubleValue) {
            builder.append(longValue);
        } else {
            builder.append(doubleValue);
        }
        builder.append('\n');
    }

    void format(StringBuilder builder) {
        builder.append(name);
        if (!labels.isEmpty()) {
            builder.append('{');
            boolean first = true;
            for (MetricLabel label : labels) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                label.format(builder);
            }
            builder.append('}');
        }
    }
}

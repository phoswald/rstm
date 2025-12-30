package com.github.phoswald.rstm.http.metrics;

import java.util.concurrent.atomic.AtomicLong;

public record Counter(MetricInstance instance, AtomicLong value) implements Metric {

    @Override
    public double calculateValue() {
        return value.doubleValue();
    }
}

package com.github.phoswald.rstm.http.metrics;

public record Gauge(MetricInstance instance, GaugeSupplier value) implements Metric {

    @Override
    public double calculateValue() throws Exception {
        return value.calculateValue();
    }
}

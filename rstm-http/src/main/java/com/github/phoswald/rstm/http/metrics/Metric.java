package com.github.phoswald.rstm.http.metrics;

public sealed interface Metric permits Counter, Gauge {

    MetricInstance instance();

    double calculateValue() throws Exception;
}

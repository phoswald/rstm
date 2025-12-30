package com.github.phoswald.rstm.http.metrics;

@FunctionalInterface
public interface GaugeSupplier {

    double calculateValue() throws Exception;
}

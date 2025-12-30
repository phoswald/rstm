package com.github.phoswald.rstm.http.metrics;

import static com.github.phoswald.rstm.http.server.HttpServerConfig.get;
import static com.github.phoswald.rstm.http.server.HttpServerConfig.route;

import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.phoswald.rstm.http.HttpResponse;
import com.github.phoswald.rstm.http.server.HttpFilter;

public class MetricsRegistry {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SortedMap<MetricInstance, Metric> metrics = new ConcurrentSkipListMap<>(MetricInstance.COMPARE);

    public Gauge registerGauge(String name, GaugeSupplier value, MetricLabel... labels) {
        return register(new Gauge(MetricInstance.create(name, labels), value));
    }

    public Counter registerCounter(String name, MetricLabel... labels) {
        return register(new Counter(MetricInstance.create(name, labels), new AtomicLong()));
    }

    private <T extends Metric> T register(T metric) {
        if(metrics.putIfAbsent(metric.instance(), metric) != null) {
            throw new IllegalArgumentException("Duplicate instance: " + metric.instance());
        }
        return metric;
    }

    public HttpFilter createRoute() {
        return route("/metrics", get(_ -> HttpResponse.text(200, collectMetrics())));
    }

    String collectMetrics() {
        var builder = new StringBuilder();
        metrics.forEach((_,metric) -> collectMetric(builder, metric));
        return builder.toString();
    }

    private void collectMetric(StringBuilder builder, Metric metric) {
        try {
            double value = metric.calculateValue();
            metric.instance().formatValue(builder, value);
        } catch(Exception e) {
            logger.warn("Exception while collecting '{}': {}", metric.instance().name(), e.toString());
        }
    }
}

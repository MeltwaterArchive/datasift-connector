package com.datasift.connector.reader;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.readytalk.metrics.StatsDReporter;

import java.util.concurrent.TimeUnit;

/**
 * Definition of metrics.
 */
@SuppressWarnings({
        "checkstyle:designforextension",
        "checkstyle:visibilitymodifier"})
public class Metrics {

    /**
     * Constructor with registry creation.
     */
    public Metrics() {
        this(new MetricRegistry());
    }

    /**
     * Constructor passing in metric registry.
     * @param registry the metric registry
     */
    public Metrics(final MetricRegistry registry) {
        this.registry = registry;
        //this.httpConnectionAttempt = registry.meter("http-connection-attempt");
    }

    /**
     * The metrics registry.
     */
    private MetricRegistry registry;

    /**
     * Gets the metrics registry.
     * @return the metrics registry
     */
    public MetricRegistry getRegistry() {
        return this.registry;
    }

    /**
     * The meter for measuring connection attempts for the HTTP endpoint.
     */
    public Meter httpConnectionAttempt;


    /**
     * Create a reporter to send metrics to Graphite.
     * @param registry the metric registry
     * @param host the host of the metrics server
     * @param port the port of the metrics server
     * @param prefix the prefix for the metrics
     * @param reportingTime the amount of time between polls
     */
    @SuppressWarnings("checkstyle:designforextension")
    public void createAndStartStatsDReporter(
            final MetricRegistry registry,
            final String host,
            final int port,
            final String prefix,
            final int reportingTime) {
        StatsDReporter.forRegistry(registry)
                .prefixedWith(prefix)
                .build(host, port)
                .start(reportingTime, TimeUnit.SECONDS);
    }
}

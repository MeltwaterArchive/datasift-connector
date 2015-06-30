package com.datasift.connector.reader;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.readytalk.metrics.StatsDReporter;
import com.twitter.hbc.core.StatsReporter;

import java.util.concurrent.TimeUnit;

/**
 * Definition of metrics.
 */
@SuppressWarnings({
        "checkstyle:designforextension",
        "checkstyle:visibilitymodifier"})
public class Metrics {

    /**
     * Constructor for the Metrics object.
     * @param stats the Gnip client StatsTracker
     */
    public Metrics(final StatsReporter.StatsTracker stats) {
        this.stats = stats;
    }

    /**
     * The metrics registry.
     */
    private final MetricRegistry registry = new MetricRegistry();

    /**
     * The Gnip client StatsTracker.
     */
    private StatsReporter.StatsTracker stats = null;

    /**
     * Gets the metrics registry.
     * @return the metrics registry
     */
    public MetricRegistry getRegistry() {
        return this.registry;
    }

    // *** Metrics raised by reader

    /**
     * The meter for measuring reads from the buffer.
     */
    @VisibleForTesting
    public Meter read = registry.meter("read");

    /**
     * The meter for measuring sends to the onward queue.
     */
    @VisibleForTesting
    public Meter sent = registry.meter("sent");

    /**
     * The meter for measuring errors sending to the onward queue.
     */
    @VisibleForTesting
    public Meter sendError = registry.meter("send-error");

    /**
     * The meter for measuring reads from the buffer.
     */
    @VisibleForTesting
    public Meter readError =
            registry.meter("read-error");

    /**
     * The meter for measuring the calls to the shutdown hook.
     */
    @VisibleForTesting
    public Meter shutdown = registry.meter("shutdown");

    /**
     * The meter for measuring whether the Gnip client has exited.
     */
    @VisibleForTesting
    public Meter disconnected =
            registry.meter("disconnected");


    // *** Metrics collected by Gnip client

    /**
     * Number of 200 http response codes in the lifetime of the client.
     */
    public Gauge<Integer> num200s = registry.register(
            "client.200s",
            new Gauge<Integer>() {
        @Override
        public Integer getValue() {
            return stats.getNum200s();
        }
    });

    /**
     * Number of 4xx http response codes in the lifetime of the client.
     */
    public Gauge<Integer> num400s = registry.register(
            "client.400s",
            new Gauge<Integer>() {
        @Override
        public Integer getValue() {
            return stats.getNum400s();
        }
    });

    /**
     * Number of 5xx http response codes in the lifetime of the client.
     */
    public Gauge<Integer> num500s = registry.register(
            "client.500s",
            new Gauge<Integer>() {
        @Override
        public Integer getValue() {
            return stats.getNum500s();
        }
    });

    /**
     * Number of messages the client has processed.
     */
    public Gauge<Long> messages = registry.register(
            "client.messages",
            new Gauge<Long>() {
        @Override
        public Long getValue() {
            return stats.getNumMessages();
        }
    });

    /**
     * Number of disconnects.
     */
    public Gauge<Integer> disconnects = registry.register(
            "client.disconnects",
            new Gauge<Integer>() {
        @Override
        public Integer getValue() {
            return stats.getNumDisconnects();
        }
    });

    /**
     * Number of connections/reconnections.
     */
    public Gauge<Integer> connections = registry.register(
            "client.connections",
            new Gauge<Integer>() {
        @Override
        public Integer getValue() {
            return stats.getNumConnects();
        }
    });

    /**
     * Number of connection failures.
     * This includes 4xxs, 5xxs, bad host names, etc.
     */
    public Gauge<Integer> connectionFailures = registry.register(
            "client.connection-failures",
            new Gauge<Integer>() {
        @Override
        public Integer getValue() {
            return stats.getNumConnectionFailures();
        }
    });

    /**
     * Number of events dropped from the event queue.
     */
    public Gauge<Long> clientEventsDropped = registry.register(
            "client.client-events-dropped",
            new Gauge<Long>() {
        @Override
        public Long getValue() {
            return stats.getNumClientEventsDropped();
        }
    });

    /**
     * Number of messages dropped from the message queue.
     * Occurs when messages in the message queue aren't
     * dequeued fast enough.
     */
    public Gauge<Long> messagesDropped = registry.register(
            "client.messages-dropped",
            new Gauge<Long>() {
        @Override
        public Long getValue() {
            return stats.getNumMessagesDropped();
        }
    });

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

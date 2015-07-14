package com.datasift.connector.writer;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
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
        this.httpConnectionAttempt = registry.meter("http-connection-attempt");
        this.httpConnectionSucceeded =
                registry.meter("http-connection-succeeded");
        this.httpConnectionFailed =
                registry.meter("http-connection-failed");
        this.httpConnectionClosed =
                registry.meter("http-connection-closed");
        this.sendAttempt = registry.meter("sent-attempt");
        this.sendSuccess = registry.meter("send-success");
        this.sentItems = registry.meter("sent-items");
        this.sendError = registry.meter("send-error");
        this.sendRateLimit = registry.meter("send-rate-limit");
        this.sendException = registry.meter("send-exception");
        this.sendLinearBackoff = registry.meter("send-backoff-linear");
        this.sendExponentialBackoff = registry.meter("send-backoff-exponential");
        this.readKafkaItem = registry.meter("read-kafka-item");
        this.passedOnKafkaItem = registry.meter("passed-on-kafka-item");
        this.readKafkaItemFromConsumer = registry.meter("read-kafka-item-from-consumer");
        this.readKafkaItemsFromConsumer = registry.meter("read-kafka-items-from-consumer");
        this.sendHeartbeatAttempt = registry.meter("send-http-heartbeat");
        this.sendHeartbeatSuccess =
                registry.meter("send-http-heartbeat-success");
        this.sendHeartbeatFailure =
                registry.meter("send-http-heartbeat-failure");
        this.interruptedService = registry.meter("interrupt-service");
        this.interruptedHTTPSending = registry.meter("interrupt-http-send");
        this.interruptedShutdown = registry.meter("interrupt-shutdown");
        this.shutdown = registry.meter("shutdown");
        this.bulkPostTime = registry.timer("bulk-post-time");
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
     * The meter for measuring succeeded connections for the HTTP endpoint.
     */
    public Meter httpConnectionSucceeded;

    /**
     * The meter for measuring failed connections for the HTTP endpoint.
     */
    public Meter httpConnectionFailed;

    /**
     * The meter for measuring HTTP connection closures.
     */
    public Meter httpConnectionClosed;

    /**
     * The meter for measuring items attempting to be sent to the HTTP endpoint.
     */
    public Meter sendAttempt;

    /**
     * The meter for measuring successful posts sent the HTTP endpoint.
     */
    public Meter sendSuccess;

    /**
     * The meter for measuring items successfully sent to the HTTP endpoint.
     */
    public Meter sentItems;

    /**
     * The meter for measuring errors sending to the HTTP endpoint.
     */
    public Meter sendError;

    /**
     * The meter for measuring the number of rate limits.
     */
    public Meter sendRateLimit;

    /**
     * The meter for measuring unexpected exceptions sending to the endpoint.
     */
    public Meter sendException;

    /**
     * The meter for measuring the rate at which the connection has backed
     * off linearly.
     */
    public Meter sendLinearBackoff;

    /**
     * The meter for measuring the rate at which the connection has backed
     * off exponentially.
     */
    public Meter sendExponentialBackoff;

    /**
     * The meter for measuring items read from Kafka.
     */
    public Meter readKafkaItem;

    /**
     * The meter for measuring items passed on from the consumer for processing.
     */
    public Meter passedOnKafkaItem;

    /**
     * The meter for measuring the rate an item is read from Kafka.
     */
    public Meter readKafkaItemFromConsumer;

    /**
     * The meter for measuring the number of times we have read a set of items
     * from Kafka.
     */
    public Meter readKafkaItemsFromConsumer;

    /**
     * The meter for measuring attempts to send HTTP stream heartbeat.
     */
    public Meter sendHeartbeatAttempt;

    /**
     * The meter for measuring number of successful HTTP stream heartbeats.
     */
    public Meter sendHeartbeatSuccess;

    /**
     * The meter for measuring number of failed HTTP stream heartbeats.
     */
    public Meter sendHeartbeatFailure;

    /**
     * The meter for measuring number of thread interrupts in service loop.
     */
    public Meter interruptedService;

    /**
     * The meter for measuring number of thread interrupts in stream manager.
     */
    public Meter interruptedHTTPSending;

    /**
     * The meter for measuring number of thread interrupts during shutdown.
     */
    public Meter interruptedShutdown;

    /**
     * The meter for measuring number of writer shutdowns.
     */
    public Meter shutdown;

    /**
     * The timer for measuring how long the bulk post takes.
     */
    public Timer bulkPostTime;

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

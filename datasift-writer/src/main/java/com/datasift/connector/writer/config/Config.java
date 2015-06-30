package com.datasift.connector.writer.config;

import javax.validation.constraints.NotNull;

/**
 * Holds & provides access to configuration properties
 * for the DataSift Writer service.
 */
@SuppressWarnings("checkstyle:visibilitymodifier")
public class Config {

    /**
     * Kafka configuration.
     */
    @NotNull
    public KafkaConfig kafka;

    /**
     * Zookeeper configuration.
     */
    @NotNull
    public ZookeeperConfig zookeeper;

    /**
     * Output stream configuration.
     */
    @NotNull
    public DataSiftConfig datasift;

    /**
     * The metrics configuration.
     */
    @NotNull
    public MetricsConfig metrics;
}

package com.datasift.connector.reader.config;

import javax.validation.constraints.NotNull;

/**
 * Holds & provides access to configuration properties
 * for the Historics Reader service.
 */
@SuppressWarnings("checkstyle:visibilitymodifier")
public class Config {

    /**
     * The Kafka configuration.
     */
    @NotNull
    public DatabaseConfig database;

    /**
     *
     */
    @NotNull
    public GnipConfig gnip;

    /**
     *
     */
    @NotNull
    public KafkaConfig kafka;

    /**
     * The metrics configuration.
     */
    @NotNull
    public MetricsConfig metrics;
}

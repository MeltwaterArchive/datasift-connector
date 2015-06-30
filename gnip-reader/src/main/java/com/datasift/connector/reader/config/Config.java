package com.datasift.connector.reader.config;

import javax.validation.constraints.NotNull;

/**
 * Holds & provides access to configuration properties
 * for the Gnip Reader service.
 */
@SuppressWarnings("checkstyle:visibilitymodifier")
public class Config {

    /**
     * The Kafka configuration.
     */
    @NotNull
    public KafkaConfig kafka;

    /**
     * The Gnip configuration.
     */
    @NotNull
    public GnipConfig gnip;

    /**
     * The metrics configuration.
     */
    @NotNull
    public MetricsConfig metrics;
}

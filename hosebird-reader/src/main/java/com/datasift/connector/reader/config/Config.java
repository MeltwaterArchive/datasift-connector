package com.datasift.connector.reader.config;

import javax.validation.constraints.NotNull;

/**
 * Holds & provides access to configuration properties
 * for the Gnip Reader service.
 */
@SuppressWarnings("checkstyle:visibilitymodifier")
public abstract class Config {

    /**
     * The Kafka configuration.
     */
    @NotNull
    public KafkaConfig kafka;

    /**
     * The Hosebird configuration.
     */
    @NotNull
    public HosebirdConfig hosebird;

    /**
     * The metrics configuration.
     */
    @NotNull
    public MetricsConfig metrics;
}

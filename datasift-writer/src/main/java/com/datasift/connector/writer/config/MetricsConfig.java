package com.datasift.connector.writer.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

/**
 * The container for the metrics configuration.
 */
@SuppressWarnings("checkstyle:visibilitymodifier")
public class MetricsConfig {

    /**
     * The host of the metrics server.
     */
    @NotNull
    public String host;

    /**
     * The port of the metrics server.
     */
    @NotNull
    public int port;

    /**
     * The prefix for Gnip Reader metrics.
     */
    @NotNull
    public String prefix;

    /**
     * The time between metric polls.
     */
    @NotNull
    @JsonProperty("reporting-time")
    public int reportingTime;
}



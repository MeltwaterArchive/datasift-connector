package com.datasift.connector.reader.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * The container for the Kafka configuration.
 */
@SuppressWarnings("checkstyle:visibilitymodifier")
public class KafkaConfig {

    /**
     * The topic name of the Kafka queue.
     */
    @SuppressWarnings("checkstyle:magicnumber")
    @JsonProperty("topic")
    @NotNull
    @Size(max = 255)
    @Pattern(regexp = "[a-zA-Z0-9\\\\._\\\\-]+")
    public String topic = "twitterapi";

    /**
     * The Kafka server addresses.
     */
    @NotNull
    public String servers = "localhost:9092";

    /**
     * The amount of time to wait before attempting to
     * retry a failed produce request to a given topic
     * partition.
     * Key defined by
     * ProducerConfig.RETRY_BACKOFF_MS_CONFIG.
     */
    @SuppressWarnings("checkstyle:magicnumber")
    @JsonProperty("retry-backoff")
    @NotNull
    public long retryBackoff = 1000;

    /**
     * The amount of time to wait before attempting to
     * reconnect to a given host when a connection fails.
     */
    @SuppressWarnings("checkstyle:magicnumber")
    @JsonProperty("reconnect-backoff")
    @NotNull
    public long reconnectBackoff = 1000;
}

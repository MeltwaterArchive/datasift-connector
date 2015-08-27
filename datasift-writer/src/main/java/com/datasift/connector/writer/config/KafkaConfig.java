package com.datasift.connector.writer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Holds & provides access to Kafka specific configuration options.
 */
@SuppressWarnings("checkstyle:visibilitymodifier")
public class KafkaConfig {

    /**
     * Default Kafka broker port number.
     */
    private static final int DEFAULT_BROKER_PORT = 9092;

    /**
     * The topic name of the Kafka queue.
     */
    @SuppressWarnings("checkstyle:magicnumber")
    @JsonProperty("topic")
    @NotNull
    @Size(max = 255)
    @Pattern(regexp = "[a-zA-Z0-9\\\\._\\\\-]+")
    public String topic = "twitter";

    /**
     * The Kafka broker address.
     */
    @NotNull
    public String broker = "localhost";

    /**
     * The Kafka broker port.
     */
    @NotNull
    public int port = DEFAULT_BROKER_PORT;
}

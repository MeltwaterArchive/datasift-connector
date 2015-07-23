package com.datasift.connector.reader.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

/**
 * Base class for the configuration used by Hosebird reader
 * variants.
 */
@SuppressWarnings("checkstyle:visibilitymodifier")
public class HosebirdConfig {
    /**
     * The number of retries the Hosebird client will
     * perform on disconnection or error. The client
     * starts at a retry interval of 5 seconds and
     * backs off exponentially to a maximum of 320 seconds,
     * so ten retries equates to just under a hour.
     */
    @SuppressWarnings("checkstyle:magicnumber")
    @NotNull
    public int retries = 10;

    /**
     * The buffer size for the reader client.
     */
    @JsonProperty("buffer_size")
    @NotNull
    public int bufferSize;

    /**
     * The timeout for the buffer to accept a new
     * item in ms. The buffer will wait if full so the
     * messages must be dequeued fast enough.
     */
    @JsonProperty("buffer_timeout")
    @SuppressWarnings("checkstyle:magicnumber")
    @NotNull
    public int bufferTimeout = 500;
}

package com.datasift.connector.reader.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.twitter.hbc.core.Constants;

import javax.validation.constraints.NotNull;

/**
 * The container for the Gnip configuration.
 */
@SuppressWarnings("checkstyle:visibilitymodifier")
public class GnipConfig {

    /**
     * The Gnip account id.
     */
    @NotNull
    public String account;

    /**
     * The label for the Gnip stream.
     */
    @NotNull
    public String label;

    /**
     * The Gnip product name.
     */
    @NotNull
    public String product;

    /**
     * The user for the Gnip account.
     */
    @NotNull
    public String username;

    /**
     * The password for the Gnip account.
     */
    @NotNull
    public String password;

    /**
     * The endpoint of the Gnip stream.
     */
    @NotNull
    public String host = Constants.ENTERPRISE_STREAM_HOST;

    /**
     * The number of retries the Gnip client will
     * perform on disconnection or error. The client
     * starts at a retry interval of 5 seconds and
     * backs off exponentially to a maximum of 320 seconds,
     * so ten retries equates to just under a hour.
     */
    @SuppressWarnings("checkstyle:magicnumber")
    @NotNull
    public int retries = 10;

    /**
     * The buffer size for the Gnip client.
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

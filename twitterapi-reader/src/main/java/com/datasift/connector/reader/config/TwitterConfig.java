package com.datasift.connector.reader.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * The container for the Twitter configuration.
 */
@SuppressWarnings("checkstyle:visibilitymodifier")
public class TwitterConfig {

    /**
     * The oauth consumer key.
     */
    @JsonProperty("consumer_key")
    @NotNull
    public String consumerKey;

    /**
     * The oauth consumer secret.
     */
    @JsonProperty("consumer_secret")
    @NotNull
    public String consumerSecret;

    /**
     * The oauth access token.
     */
    @JsonProperty("access_token")
    @NotNull
    public String accessToken;

    /**
     * The oauth access secret.
     */
    @JsonProperty("access_secret")
    @NotNull
    public String accessSecret;

    /**
     * Some keywords to filter on.
     */
    @JsonProperty("keywords")
    @NotNull
    public List<String> keywords;

    /**
     * Some keywords to filter on.
     */
    @JsonProperty("user_ids")
    @NotNull
    public List<Long> userIds;

    /**
     * The number of retries the client will
     * perform on disconnection or error. The client
     * starts at a retry interval of 5 seconds and
     * backs off exponentially to a maximum of 320 seconds,
     * so ten retries equates to just under a hour.
     */
    @SuppressWarnings("checkstyle:magicnumber")
    @NotNull
    public int retries = 10;

    /**
     * The buffer size for the client.
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

package com.datasift.connector.reader.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * The container for the Twitter configuration.
 */
@SuppressWarnings("checkstyle:visibilitymodifier")
public class TwitterApiConfig {

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
}

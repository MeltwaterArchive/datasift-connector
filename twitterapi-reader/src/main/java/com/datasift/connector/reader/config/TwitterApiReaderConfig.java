package com.datasift.connector.reader.config;

import javax.validation.constraints.NotNull;

/**
 * The configuration for the Twitter Api reader.
 */
@SuppressWarnings("checkstyle:visibilitymodifier")
public class TwitterApiReaderConfig extends Config {

    /**
     * The TwitterApi configuration.
     */
    @NotNull
    public TwitterApiConfig twitterapi;
}

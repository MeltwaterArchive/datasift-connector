package com.datasift.connector.reader.config;

import javax.validation.constraints.NotNull;

/**
 * The configuration for the Gnip Reader.
 */
@SuppressWarnings("checkstyle:visibilitymodifier")
public class GnipReaderConfig extends Config {

    /**
     * The Gnip configuration.
     */
    @NotNull
    public GnipConfig gnip;
}

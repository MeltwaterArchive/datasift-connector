package com.datasift.connector.reader.config;

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
    public String host = Constants.ENTERPRISE_STREAM_HOST_v2;
}

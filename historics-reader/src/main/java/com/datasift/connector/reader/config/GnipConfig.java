package com.datasift.connector.reader.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

/**
 * The container for the Database configuration.
 */
@SuppressWarnings("checkstyle:visibilitymodifier")
public class GnipConfig {

    /**
     * Host on which output HTTP API is exposed.
     */
    @NotNull
    @JsonProperty("base_url")
    public String baseURL;

    /**
     * Port on which output HTTP API is exposed.
     */
    @NotNull
    public Integer port;

    /**
     *
     */
    @NotNull
    @JsonProperty("account_name")
    public String accountName;

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
}

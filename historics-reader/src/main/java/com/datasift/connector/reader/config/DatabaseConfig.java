package com.datasift.connector.reader.config;

import javax.validation.constraints.NotNull;

/**
 * The container for the Database configuration.
 */
@SuppressWarnings("checkstyle:visibilitymodifier")
public class DatabaseConfig {

    /**
     *
     */
    @NotNull
    public String filepath = "historics-api/db.sqlite";
}

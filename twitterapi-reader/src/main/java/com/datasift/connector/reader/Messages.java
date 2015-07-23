package com.datasift.connector.reader;

/**
 * Defines globally available message constants for logging.
 */
@SuppressWarnings("checkstyle:visibilitymodifier")
public final class Messages {

    /**
     * Prevent construction.
     */
    private Messages() { }

    /**
     * Configuration message stating file is not valid JSON.
     */
    public static final String CONFIG_NOT_JSON =
            "Error parsing config provided. Configuration file must"
            + " be valid JSON & adhere to format set in configuration"
            + " documentation";

    /**
     * Configuration message stating required items are missing.
     */
    public static final String CONFIG_MISSING_ITEMS =
            "Empty or missing configuration options provided."
            + "Configuration file must adhere to format set in"
            + " configuration documentation.";

    /**
     * Configuration message stating file is not readable.
     */
    public static final String CONFIG_NOT_READABLE =
            "Error reading configuration file provided";

    /**
     * Service message stating command line usage is incorrect.
     */
    public static final String EXIT_ARGUMENTS_EMPTY =
            "No arguments provided. Usage: java -cp './*'"
            + " com.datasift.connector.TwitterApiReader"
            + " /etc/datasift/reader.json";

    /**
     * Service message stating that configuration parsing failed.
     */
    public static final String EXIT_CONFIG =
            "Service shutting down due to configuration error";
}

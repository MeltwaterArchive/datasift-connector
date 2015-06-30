package com.datasift.connector.writer;

/**
 * Defines globally available message constants for logging.
 */
public final class Messages {

    /**
     * Prevent construction.
     */
    private Messages() { }

    // Configuration loading
    /**
     * Configuration message stating file is not valid JSON.
     */
    public static final String CONFIG_NOT_JSON =
            "Error parsing config provided. "
            + "Configuration file must be valid JSON & adhere to "
            + "format set in configuration documentation";

    /**
     * Configuration message stating required items are missing.
     */
    public static final String CONFIG_MISSING_ITEMS =
            "Empty or missing configuration options provided. "
            + "Configuration file must adhere to format set in "
            + "configuration documentation";

    /**
     * Configuration message stating file is not readable.
     */
    public static final String CONFIG_NOT_READABLE =
            "Error reading configuration file provided";

    // Service shut-down
    /**
     * Service message stating command line usage is incorrect.
     */
    public static final String EXIT_ARGUMENTS_EMPTY =
            "No arguments provided. "
            + "Usage: java -cp './*' "
            + "com.datasift.connector.DataSiftWriter "
            + "/etc/datasift/writer.json";

    /**
     * Service message stating that configuration parsing failed.
     */
    public static final String EXIT_CONFIG =
            "Service shutting down due to configuration error";
}

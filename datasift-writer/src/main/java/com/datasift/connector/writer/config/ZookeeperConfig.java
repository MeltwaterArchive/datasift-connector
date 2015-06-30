package com.datasift.connector.writer.config;

/**
 * Holds & provides access to Zookeeper specific configuration options.
 */
@SuppressWarnings("checkstyle:visibilitymodifier")
public class ZookeeperConfig {

    /**
     * Socket on which Zookeeper is running.
     * In the form host:port
     */
    public String socket = "http://localhost:2181";
}

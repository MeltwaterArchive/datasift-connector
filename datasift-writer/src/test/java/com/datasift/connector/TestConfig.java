package com.datasift.connector;

import com.datasift.connector.writer.config.Config;
import com.datasift.connector.writer.config.KafkaConfig;
import com.datasift.connector.writer.config.DataSiftConfig;
import com.datasift.connector.writer.config.ZookeeperConfig;

import org.junit.Test;

public class TestConfig {
    @Test
    public void can_set_properties() {
        Config c = new Config();
        c.kafka = new KafkaConfig();
        c.datasift = new DataSiftConfig();
        c.zookeeper = new ZookeeperConfig();
    }
}

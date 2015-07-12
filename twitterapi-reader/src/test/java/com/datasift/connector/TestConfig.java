package com.datasift.connector;

import com.datasift.connector.reader.config.Config;
import com.datasift.connector.reader.config.MetricsConfig;
import com.datasift.connector.reader.config.KafkaConfig;
import com.datasift.connector.reader.config.TwitterConfig;
import org.junit.Test;

public class TestConfig {
    @Test
    public void can_set_properties() {
        Config c = new Config();
        c.kafka = new KafkaConfig();
        c.twitter = new TwitterConfig();
        c.metrics = new MetricsConfig();
    }
}

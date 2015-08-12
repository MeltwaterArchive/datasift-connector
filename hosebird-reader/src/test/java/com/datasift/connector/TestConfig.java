package com.datasift.connector;

import com.datasift.connector.reader.config.Config;
import com.datasift.connector.reader.config.KafkaConfig;
import com.datasift.connector.reader.config.HosebirdConfig;
import com.datasift.connector.reader.config.MetricsConfig;
import org.junit.Test;

public class TestConfig {
    @Test
    public void can_set_properties() {
        Config c = new ConcreteHosebirdConfig();
        c.kafka = new KafkaConfig();
        c.hosebird = new HosebirdConfig();
        c.metrics = new MetricsConfig();
    }
}

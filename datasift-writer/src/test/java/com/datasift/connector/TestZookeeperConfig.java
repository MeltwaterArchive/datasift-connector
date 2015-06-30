package com.datasift.connector;

import com.datasift.connector.writer.config.ZookeeperConfig;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TestZookeeperConfig {

    @Test
    public void should_set_defaults_on_construction() {
        ZookeeperConfig zc = new ZookeeperConfig();
        assertEquals("http://localhost:2181", zc.socket);
    }
}

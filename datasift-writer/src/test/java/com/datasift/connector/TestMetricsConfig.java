package com.datasift.connector;

import com.datasift.connector.writer.config.MetricsConfig;
import org.junit.Test;

public class TestMetricsConfig {

    @Test
    public void can_set_properties() {
        MetricsConfig gc = new MetricsConfig();
        gc.host = "HOST";
        gc.port = 84;
        gc.prefix = "PREFIX";
        gc.reportingTime = 1;
    }
}

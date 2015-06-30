package com.datasift.connector.writer;

import com.codahale.metrics.MetricRegistry;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestMetrics {
    @Test
    public void can_get_metrics_registry() {
        Metrics metrics = new Metrics();
        assertNotNull(metrics.getRegistry());
        assert(metrics.getRegistry() instanceof MetricRegistry);
    }
}

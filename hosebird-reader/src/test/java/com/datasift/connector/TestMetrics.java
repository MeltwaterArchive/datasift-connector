package com.datasift.connector;

import com.codahale.metrics.MetricRegistry;
import com.datasift.connector.reader.Metrics;
import com.twitter.hbc.core.StatsReporter;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class TestMetrics {

    @Test
    public void should_create_statsd_reporter_without_error() {
        // The statsd reporter builder is static which means Mockito can't mock it.
        StatsReporter.StatsTracker stats = new StatsReporter().getStatsTracker();
        Metrics m = new Metrics(stats);
        MetricRegistry registry = mock(MetricRegistry.class);
        m.createAndStartStatsDReporter(registry, "localhost", 42424, "PREFIX", 1000);
    }
}

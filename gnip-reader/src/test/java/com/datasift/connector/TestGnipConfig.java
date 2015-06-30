package com.datasift.connector;

import com.datasift.connector.reader.config.GnipConfig;
import com.twitter.hbc.core.Constants;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestGnipConfig {

    @Test
    public void should_set_defaults_on_construction() {
        GnipConfig tc = new GnipConfig();
        assertEquals(Constants.ENTERPRISE_STREAM_HOST, tc.host);
        assertEquals(10, tc.retries);
        assertEquals(500, tc.bufferTimeout);
    }
}

package com.datasift.connector;

import com.datasift.connector.reader.config.TwitterConfig;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestTwitterConfig {

    @Test
    public void should_set_defaults_on_construction() {
        TwitterConfig tc = new TwitterConfig();
        assertEquals(10, tc.retries);
        assertEquals(500, tc.bufferTimeout);
    }
}

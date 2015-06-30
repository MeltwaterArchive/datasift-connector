package com.datasift.connector.writer;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestSleeper {

    @Test
    public void should_sleep() throws InterruptedException {
        long before = System.currentTimeMillis();
        new Sleeper().sleep(1000);
        long after = System.currentTimeMillis();
        long diff = after - before;
        assertTrue(diff >= 1000);
    }
}

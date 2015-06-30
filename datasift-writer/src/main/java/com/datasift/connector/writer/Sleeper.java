package com.datasift.connector.writer;

import java.util.concurrent.TimeUnit;

/**
 * Implements sleeping.
 * A wrapper for testing.
 */
public class Sleeper {

    /**
     * Sleep for the given number of milliseconds.
     * @param duration how long to sleep for in milliseconds
     * @throws InterruptedException if the sleep is interrupted
     */
    @SuppressWarnings("checkstyle:designforextension")
    public void sleep(final long duration) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(duration);
    }
}

package com.datasift.connector.writer;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * Allows backing off a call, either exponentially or linearly.
 */
public class Backoff {

    /**
     * The Metrics container.
     */
    private Metrics metrics;

    /**
     * The logger.
     */
    private final Logger log;

    /**
     * Implements sleeping the thread.
     * A wrapper for testing.
     */
    private final Sleeper sleeper;

    /**
     * The types of backoff.
     */
    public enum BackoffType {

        /**
         * Exponential backoff.
         */
        EXPONENTIAL,

        /**
         * Linear backoff.
         */
        LINEAR,

        /**
         * Wait until a specified time.
         */
        WAIT_UNTIL
    }

    /**
     * The current backoff type.
     */
    private BackoffType currentType = BackoffType.EXPONENTIAL;

    /**
     * Milliseconds to wait when initially backing off linearly.
     */
    protected static final int LINEAR_BACKOFF = 1000;

    /**
     * Maximum milliseconds to wait for when backing off linearly.
     */
    protected static final int LINEAR_BACKOFF_MAX = 10000;

    /**
     * Milliseconds by which to increment linear backoff.
     */
    protected static final int LINEAR_BACKOFF_INC = 1000;

    /**
     * Milliseconds to wait when initially exponentially backing off.
     */
    protected static final int EXPONENTIAL_BACKOFF = 1000;

    /**
     * Maximum milliseconds to wait for when exponentially backing off.
     */
    protected static final int EXPONENTIAL_BACKOFF_MAX = 10000;

    /**
     * Multiplier by which to increment exponential exponential backoff.
     */
    protected static final int EXPONENTIAL_BACKOFF_INC = 2;

    /**
     * Get the current backoff type.
     * @return the backoff type
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected BackoffType getBackoffType() {
        return currentType;
    }

    /**
     * The amount of time to wait for on a linear backoff.
     */
    private int linearWaitFor = 0;

    /**
     * Gets the linear wait for time.
     * @return the time to wait for
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected final int getLinearWaitFor() {
        return this.linearWaitFor;
    }

    /**
     * The amount of time to wait on an exponential backoff.
     */
    private int exponentialWaitFor = EXPONENTIAL_BACKOFF / 2;

    /**
     * Gets the exponential wait for time.
     * @return the time to wait for
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected final int getExponentialWaitFor() {
        return this.exponentialWaitFor;
    }

    /**
     * The constructor.
     * @param sleeper allows sleeping the thread
     * @param log the logger
     * @param metrics the metrics
     */
    public Backoff(
            final Sleeper sleeper,
            final Logger log,
            final Metrics metrics) {
        this.sleeper = sleeper;
        this.log = log;
        this.metrics = metrics;
    }

    /**
     * Backoff linearly.
     * @throws InterruptedException if the sleep is interrupted
     */
    @SuppressWarnings("checkstyle:designforextension")
    public void linearBackoff() throws InterruptedException {
        if (currentType != BackoffType.LINEAR) {
            reset();
        }

        currentType = BackoffType.LINEAR;
        linearWaitFor += LINEAR_BACKOFF_INC;
        if (linearWaitFor > LINEAR_BACKOFF_MAX) {
            linearWaitFor = LINEAR_BACKOFF_MAX;
        }

        metrics.sendLinearBackoff.mark();
        log.info("Linear backoff for {} ms", linearWaitFor);
        sleeper.sleep(linearWaitFor);
    }

    /**
     * Backoff exponentially.
     * @throws InterruptedException if the sleep is interrupted.
     */
    @SuppressWarnings("checkstyle:designforextension")
    public void exponentialBackoff() throws InterruptedException {
        if (currentType != BackoffType.EXPONENTIAL) {
            reset();
        }

        currentType = BackoffType.EXPONENTIAL;

        exponentialWaitFor *= EXPONENTIAL_BACKOFF_INC;
        if (exponentialWaitFor > EXPONENTIAL_BACKOFF_MAX) {
            exponentialWaitFor = EXPONENTIAL_BACKOFF_MAX;
        }

        metrics.sendExponentialBackoff.mark();
        log.info("Exponential backoff for {} ms", exponentialWaitFor);
        sleeper.sleep(exponentialWaitFor);
    }

     /**
     * Wait until the TTL has been reached.
     * @param waitUntil the timestamp in seconds of the next reset
     * @throws InterruptedException throw if sleep interrupted
     */
     @SuppressWarnings("checkstyle:designforextension")
    public void waitUntil(final long waitUntil) throws InterruptedException {
         this.currentType = BackoffType.WAIT_UNTIL;
         long waitFor = TimeUnit.SECONDS.toMillis(waitUntil) - System.currentTimeMillis();
         if (waitFor > 0) {
             log.info("Waiting for {} ms", waitFor);
             sleeper.sleep(waitFor);
         }
    }

    /**
     * Resets the backoff time.
     */
    public final void reset() {
        log.debug("Reset backoff time");
        exponentialWaitFor = EXPONENTIAL_BACKOFF / 2;
        linearWaitFor = 0;
    }
}

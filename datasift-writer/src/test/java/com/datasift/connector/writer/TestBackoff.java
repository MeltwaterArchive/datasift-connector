package com.datasift.connector.writer;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Meter;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class TestBackoff {

    @Test
    public void should_switch_type_on_linear_backoff() throws InterruptedException {
        Logger loggger = mock(Logger.class);
        Metrics metrics = new Metrics();
        Sleeper sleeper = mock(Sleeper.class);

        Backoff b = new Backoff(sleeper, loggger, metrics);
        b.exponentialBackoff();
        assertEquals(Backoff.BackoffType.EXPONENTIAL, b.getBackoffType());
        b.linearBackoff();
        assertEquals(Backoff.BackoffType.LINEAR, b.getBackoffType());
    }

    @Test
    public void should_switch_type_on_exponential_backoff() throws InterruptedException {
        Logger loggger = mock(Logger.class);
        Metrics metrics = new Metrics();
        Sleeper sleeper = mock(Sleeper.class);

        Backoff b = new Backoff(sleeper, loggger, metrics);
        b.linearBackoff();
        assertEquals(Backoff.BackoffType.LINEAR, b.getBackoffType());
        b.exponentialBackoff();
        assertEquals(Backoff.BackoffType.EXPONENTIAL, b.getBackoffType());
    }

    @Test
    public void should_backoff_linearlly() throws InterruptedException {
        Logger loggger = mock(Logger.class);
        Metrics metrics = new Metrics();
        Sleeper sleeper = mock(Sleeper.class);

        Backoff b = new Backoff(sleeper, loggger, metrics);
        b.linearBackoff();
        verify(sleeper).sleep(Backoff.LINEAR_BACKOFF);
        b.linearBackoff();
        verify(sleeper).sleep(Backoff.LINEAR_BACKOFF + Backoff.LINEAR_BACKOFF_INC);
        b.linearBackoff();
        verify(sleeper).sleep(Backoff.LINEAR_BACKOFF + 2 * Backoff.LINEAR_BACKOFF_INC);
        b.linearBackoff();
        verify(sleeper).sleep(Backoff.LINEAR_BACKOFF + 3 * Backoff.LINEAR_BACKOFF_INC);
        b.linearBackoff();
        verify(sleeper).sleep(Backoff.LINEAR_BACKOFF + 4 * Backoff.LINEAR_BACKOFF_INC);
        b.linearBackoff();
        verify(sleeper).sleep(Backoff.LINEAR_BACKOFF + 5 * Backoff.LINEAR_BACKOFF_INC);
        b.linearBackoff();
        verify(sleeper).sleep(Backoff.LINEAR_BACKOFF + 6 * Backoff.LINEAR_BACKOFF_INC);
        b.linearBackoff();
        verify(sleeper).sleep(Backoff.LINEAR_BACKOFF + 7 * Backoff.LINEAR_BACKOFF_INC);
        b.linearBackoff();
        verify(sleeper).sleep(Backoff.LINEAR_BACKOFF + 8 * Backoff.LINEAR_BACKOFF_INC);
        b.linearBackoff();
        verify(sleeper).sleep(Backoff.LINEAR_BACKOFF + 9 * Backoff.LINEAR_BACKOFF_INC);
        b.linearBackoff();
        verify(sleeper, times(2)).sleep(Backoff.LINEAR_BACKOFF_MAX);
        b.linearBackoff();
    }

    @Test
    public void should_backoff_exponentially() throws InterruptedException {
        Logger loggger = mock(Logger.class);
        Metrics metrics = new Metrics();
        Sleeper sleeper = mock(Sleeper.class);

        Backoff b = new Backoff(sleeper, loggger, metrics);
        b.exponentialBackoff();
        verify(sleeper).sleep(1000);
        b.exponentialBackoff();
        verify(sleeper).sleep(2000);
        b.exponentialBackoff();
        verify(sleeper).sleep(4000);
        b.exponentialBackoff();
        verify(sleeper).sleep(8000);
        b.exponentialBackoff();
        verify(sleeper).sleep(10000);
        b.exponentialBackoff();
        verify(sleeper, times(2)).sleep(10000);
    }

    @Test
    public void reset_should_reset_backoffs() throws InterruptedException {
        Logger loggger = mock(Logger.class);
        Metrics metrics = new Metrics();
        Sleeper sleeper = mock(Sleeper.class);

        Backoff b = new Backoff(sleeper, loggger, metrics);
        b.exponentialBackoff();
        b.reset();
        assertEquals(Backoff.EXPONENTIAL_BACKOFF / 2, b.getExponentialWaitFor());

        b.linearBackoff();
        b.reset();
        assertEquals(0, b.getLinearWaitFor());
    }

    @Test
    public void should_reset_when_changing_type() throws InterruptedException {
        Logger loggger = mock(Logger.class);
        Metrics metrics = new Metrics();
        Sleeper sleeper = mock(Sleeper.class);

        Backoff b = new Backoff(sleeper, loggger, metrics);
        b.exponentialBackoff();
        b.linearBackoff();
        assertEquals(Backoff.EXPONENTIAL_BACKOFF / 2, b.getExponentialWaitFor());

        b.exponentialBackoff();
        assertEquals(0, b.getLinearWaitFor());
    }

    @Test
    public void should_wait_until_correct_time() throws InterruptedException {
        Logger loggger = mock(Logger.class);
        Metrics metrics = new Metrics();
        Sleeper sleeper = mock(Sleeper.class);

        Backoff b = new Backoff(sleeper, loggger, metrics);
        long now = System.currentTimeMillis();
        b.waitUntil(TimeUnit.MILLISECONDS.toSeconds(now) + 1);
        ArgumentCaptor<Long> arg = ArgumentCaptor.forClass(Long.class);
        verify(sleeper).sleep(arg.capture());
        assertTrue(arg.getValue() < 2000 && arg.getValue() > 0);
    }

    @Test
    public void should_set_type_on_wait_until() throws InterruptedException {
        Logger logger = mock(Logger.class);
        Metrics metrics = new Metrics();
        Sleeper sleeper = mock(Sleeper.class);

        Backoff b = new Backoff(sleeper, logger, metrics);
        b.waitUntil(0);

        assertEquals(Backoff.BackoffType.WAIT_UNTIL, b.getBackoffType());
    }

    @Test
    public void should_mark_metric_for_linear_backoff() throws InterruptedException {
        Logger loggger = mock(Logger.class);
        Metrics metrics = new Metrics();
        Sleeper sleeper = mock(Sleeper.class);

        Backoff b = new Backoff(sleeper, loggger, metrics);
        b.linearBackoff();
        assertEquals(1, metrics.sendLinearBackoff.getCount());
    }

    @Test
    public void should_mark_metric_for_exponential_backoff() throws InterruptedException {
        Logger loggger = mock(Logger.class);
        Metrics metrics = new Metrics();
        Sleeper sleeper = mock(Sleeper.class);

        Backoff b = new Backoff(sleeper, loggger, metrics);
        b.exponentialBackoff();
        assertEquals(1, metrics.sendExponentialBackoff.getCount());
    }
}

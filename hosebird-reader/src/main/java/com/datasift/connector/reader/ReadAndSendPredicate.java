package com.datasift.connector.reader;

/**
 * Interface used to encapsulate whether the read and
 * send loop should continue. Used by the main loop
 * which asks the Gnip client whether to continue
 * and also used by the shutdown hook which checks whether
 * the buffer queue has been drained.
 */
public interface ReadAndSendPredicate {

    /**
     * Should the read and send continue processing?
     * @return the result
     */
    boolean process();
}


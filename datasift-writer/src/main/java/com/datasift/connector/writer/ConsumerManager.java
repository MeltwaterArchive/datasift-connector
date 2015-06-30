package com.datasift.connector.writer;

/**
 * Defines the base API for a consumer manager.
 * Allows reading of Kafka items, manual committing of offsets,
 * and resetting of queue read position.
 */
interface ConsumerManager {

    /**
     * Get the next item from the Kafka queue containing the offset and current message.
     * @return consumed data item as ConsumerData object or null if no item to consume
     */
    ConsumerData readItem();

    /**
     * Commit the last read item offset to Kafka. The next item read after a successful commit will
     * succeed the last read in the Kafka queue. Items read before the commit will no longer be
     * readable.
     * @return boolean indicating whether the commit was successful
     */
    boolean commit();
}

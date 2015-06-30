package com.datasift.connector.writer;

/**
 * Wrapper class for Kafka queue data containing a queue message and the corresponding offset.
 */
public class ConsumerData {

    /**
     * Kafka offset for this consumed item.
     */
    private long offset;

    /**
     * String data for this consumed item.
     */
    private String message;

    /**
     * Constructor. Sets offset and message members.
     * @param offset offset of item in Kafka queue
     * @param message queue data message represented as a String type
     */
    public ConsumerData(final long offset, final String message) {
        this.offset = offset;
        this.message = message;
    }

    /**
     * Gets this item's Kafka queue offset.
     * @return the queue offset as a long
     */
    public final long getOffset() {
        return offset;
    }

    /**
     * Gets this item's message data.
     * @return message data stored as a String
     */
    @SuppressWarnings("checkstyle:designforextension")
    public String getMessage() {
        return message;
    }
}

package com.datasift.connector.writer;

/**
 * Container class to return values read.
 */
public class BulkReadValues {

    /**
     * The number of items read.
     */
    private int itemsRead;

    /**
     * The data read.
     */
    private String data;

    /**
     * Get the number of items read.
     * @return the number of items read
     */
    public final int getItemsRead() {
        return this.itemsRead;
    }

    /**
     * Get the data.
     * @return the data
     */
    public final String getData() {
        return this.data;
    }

    /**
     * The constructor.
     * @param itemsRead the number of items read
     * @param data the data
     */
    public BulkReadValues(final int itemsRead, final String data) {
        this.itemsRead = itemsRead;
        this.data = data;
    }
}

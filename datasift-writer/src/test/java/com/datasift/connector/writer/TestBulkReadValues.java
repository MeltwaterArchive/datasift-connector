package com.datasift.connector.writer;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestBulkReadValues {

    @Test
    public void can_set_and_get_properties() {
        BulkReadValues brv = new BulkReadValues(42, "DATA");
        assertEquals(42, brv.getItemsRead());
        assertEquals("DATA", brv.getData());
    }
}

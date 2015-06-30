package com.datasift.connector;

import com.datasift.connector.writer.config.DataSiftConfig;
import org.junit.Test;

public class TestDataSiftConfig {

    @Test
    public void can_set_propeties() {
        DataSiftConfig dsc = new DataSiftConfig();
        dsc.apiKey = "KEY";
        dsc.baseURL = "http://a.com";
        dsc.bulkSize = 2;
        dsc.bulkInterval = 1000;
        dsc.port = 42;
        dsc.sourceID = "SOURCEID";
        dsc.username = "USER";
    }
}

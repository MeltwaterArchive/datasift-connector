package com.datasift.connector;

import com.datasift.connector.reader.config.TwitterApiConfig;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class TestTwitterConfig {

    @Test
    public void can_set_properties() {
        TwitterApiConfig tc = new TwitterApiConfig();
        tc.accessSecret = "SECRET";
        tc.accessToken = "TOKEN";
        tc.consumerKey = "KEY";
        tc.consumerSecret = "SECRET";
        ArrayList<String> keywords = new ArrayList<>();
        keywords.add(0, "word");
        tc.keywords = keywords;
        ArrayList<Long> ids = new ArrayList<>();
        ids.add(0, 1L);
        tc.userIds = ids;
    }
}

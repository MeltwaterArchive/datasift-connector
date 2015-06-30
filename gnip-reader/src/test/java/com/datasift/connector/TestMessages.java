package com.datasift.connector;

import com.datasift.connector.reader.Messages;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestMessages {
    @Test
    public void should_have_correct_messages() {
        assertEquals(
                "Error parsing config provided. Configuration file must"
                        + " be valid JSON & adhere to format set in configuration"
                        + " documentation",
                Messages.CONFIG_NOT_JSON);

        assertEquals(
                "Empty or missing configuration options provided."
                        + "Configuration file must adhere to format set in"
                        + " configuration documentation.",
                Messages.CONFIG_MISSING_ITEMS);

        assertEquals(
                "Error reading configuration file provided",
                Messages.CONFIG_NOT_READABLE);

        assertEquals(
                "No arguments provided. Usage: java -cp './*'"
                        + " com.datasift.connector.GnipReader"
                        + " /etc/datasift/reader.json",
                Messages.EXIT_ARGUMENTS_EMPTY);

        assertEquals(
                "Service shutting down due to configuration error",
                Messages.EXIT_CONFIG);
    }
}

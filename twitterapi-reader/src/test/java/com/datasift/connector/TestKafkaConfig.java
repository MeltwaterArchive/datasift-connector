package com.datasift.connector;

import com.datasift.connector.reader.config.KafkaConfig;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import java.util.Set;

import static org.junit.Assert.*;

public class TestKafkaConfig {
    @Test
    public void can_set_properties() {
        KafkaConfig kc = new KafkaConfig();
        assertEquals("twitterapi", kc.topic);
        assertEquals("localhost:9092", kc.servers);
        assertEquals(1000, kc.retryBackoff);
        assertEquals(1000, kc.reconnectBackoff);
        kc.topic = "TOPIC";
        kc.servers = "SERVERS";
        kc.reconnectBackoff = 42;
    }

    @Test
    public void should_not_validate_if_topic_name_has_invalid_characters() {
        KafkaConfig kc = new KafkaConfig();
        kc.topic = "SPACES INVALID";
        Set<ConstraintViolation<KafkaConfig>> problems =
                Validation.buildDefaultValidatorFactory()
                        .getValidator()
                        .validate(kc);
        assertEquals(1, problems.size());
        assertEquals("must match \"[a-zA-Z0-9\\._\\-]+\"", problems.iterator().next().getMessage());
    }

    @Test
    public void should_not_validate_if_servers_null() {
        KafkaConfig kc = new KafkaConfig();
        kc.servers = null;
        Set<ConstraintViolation<KafkaConfig>> problems =
                Validation.buildDefaultValidatorFactory()
                        .getValidator()
                        .validate(kc);
        assertEquals(1, problems.size());
        assertEquals("may not be null", problems.iterator().next().getMessage());
    }

    @Test
    public void should_not_validate_if_topic_name_is_longer_than_255_chars() {
        KafkaConfig kc = new KafkaConfig();
        kc.topic =
              "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789";
        Set<ConstraintViolation<KafkaConfig>> problems =
                Validation.buildDefaultValidatorFactory()
                        .getValidator()
                        .validate(kc);
        assertEquals(1, problems.size());
        assertEquals("size must be between 0 and 255", problems.iterator().next().getMessage());

    }
}

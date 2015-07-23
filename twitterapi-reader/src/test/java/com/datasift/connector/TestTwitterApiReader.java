package com.datasift.connector;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.datasift.connector.reader.Messages;
import com.datasift.connector.reader.Metrics;
import com.datasift.connector.reader.ReadAndSendPredicate;
import com.datasift.connector.reader.config.Config;
import com.datasift.connector.reader.config.HosebirdConfig;
import com.datasift.connector.reader.config.KafkaConfig;
import com.datasift.connector.reader.config.MetricsConfig;
import com.datasift.connector.reader.config.TwitterApiConfig;
import com.datasift.connector.reader.config.TwitterApiReaderConfig;
import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.StatsReporter;
import com.twitter.hbc.core.endpoint.StreamingEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.event.EventType;
import com.twitter.hbc.core.processor.HosebirdMessageProcessor;
import com.twitter.hbc.core.processor.LineStringProcessor;
import com.twitter.hbc.httpclient.BasicClient;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.BasicAuth;
import kafka.api.FetchRequest;
import kafka.api.FetchRequestBuilder;
import kafka.javaapi.FetchResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.message.Message;
import kafka.message.MessageAndOffset;
import org.apache.kafka.clients.producer.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


public class TestTwitterApiReader {

    private BasicClient client = null;
    private ClientBuilder cb = null;
    private Logger logger = null;
    private TwitterApiReaderConfig config = null;
    private ReadAndSendPredicate clientReadAndSendPredicate = null;

    @Before
    public void setup() {
        this.client = mock(BasicClient.class);
        this.cb = mock(ClientBuilder.class);
        when(cb.name("Twitter Api Reader")).thenReturn(cb);
        when(cb.hosts(Constants.STREAM_HOST)).thenReturn(cb);
        when(cb.endpoint(any(StreamingEndpoint.class))).thenReturn(cb);
        when(cb.authentication(any(Authentication.class))).thenReturn(cb);
        when(cb.processor(any(HosebirdMessageProcessor.class))).thenReturn(cb);
        when(cb.retries(10)).thenReturn(cb);
        when(cb.build()).thenReturn(client);
        this.logger = mock(Logger.class);
        this.clientReadAndSendPredicate = new ReadAndSendPredicate() {
                @Override
                public boolean process() {
                    return !client.isDone();
                }
            };

        this.config = new TwitterApiReaderConfig();
        this.config.hosebird = new HosebirdConfig();
        this.config.twitterapi = new TwitterApiConfig();
        this.config.twitterapi.consumerKey = "aaaaa";
        this.config.twitterapi.consumerSecret = "bbbbb";
        this.config.twitterapi.accessToken = "ccccc";
        this.config.twitterapi.accessSecret = "ddddd";
        this.config.hosebird.retries = 10;
        this.config.hosebird.bufferSize = 10000;
        this.config.kafka = new KafkaConfig();
        this.config.kafka.topic = "Data";
        this.config.metrics = new MetricsConfig();
        this.config.metrics.host = "G_HOST";
        this.config.metrics.port = 1111;
        this.config.metrics.prefix = "G_PREFIX";
        this.config.metrics.reportingTime = 2;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_parse_valid_config() {
        Producer<String, String> producer = mock(Producer.class);
        TwitterApiReader tr = mock(TwitterApiReader.class);
        when(tr.getHosebirdClient(any(LinkedBlockingQueue.class), any(TwitterApiReaderConfig.class))).thenCallRealMethod();
        when(tr.getKafkaProducer(any(KafkaConfig.class))).thenReturn(producer);
        when(tr.getClientBuilder()).thenReturn(this.cb);
        doNothing().when(tr).readAndSend(any(LinkedBlockingQueue.class), anyString(), any(Producer.class), any(ReadAndSendPredicate.class));
        when(tr.parseConfigFile(anyString())).thenCallRealMethod();
        when(tr.getConfigClass()).thenCallRealMethod();

        Logger logger = mock(Logger.class);
        tr.setLogger(logger);
        String workingDir = System.getProperty("user.dir");
        TwitterApiReaderConfig config = (TwitterApiReaderConfig) tr.parseConfigFile(workingDir + "/src/test/resources/Valid.json");
        verify(logger, never()).error(anyString());
        assertEquals("aaaa", config.twitterapi.consumerKey);
        assertEquals("bbbb", config.twitterapi.consumerSecret);
        assertEquals("cccc", config.twitterapi.accessToken);
        assertEquals("dddd", config.twitterapi.accessSecret);
        assertEquals(Lists.newArrayList("datasift"), config.twitterapi.keywords);
        assertEquals(Lists.newArrayList(Long.parseLong("15282408")), config.twitterapi.userIds);
        assertEquals(10, config.hosebird.retries);
        assertEquals(10000, config.hosebird.bufferSize);
        assertEquals(500, config.hosebird.bufferTimeout);
        assertEquals("twitterapi", config.kafka.topic);
        assertEquals("localhost:9092", config.kafka.servers);
        assertEquals(1001, config.kafka.retryBackoff);
        assertEquals(1002, config.kafka.reconnectBackoff);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getHosebirdCient_should_create_client_correctly() {
        Producer<String, String> producer = mock(Producer.class);
        TwitterApiReader tr = mock(TwitterApiReader.class);
        reset(this.logger);
        tr.setLogger(this.logger);
        when(tr.getMetrics(any(StatsReporter.StatsTracker.class))).thenCallRealMethod();
        when(tr.getHosebirdClient(any(LinkedBlockingQueue.class), any(Config.class))).thenCallRealMethod();
        when(tr.getKafkaProducer(any(KafkaConfig.class))).thenReturn(producer);
        when(tr.getClientBuilder()).thenReturn(this.cb);
        when(tr.parseConfigFile(anyString())).thenReturn(config);
        when(tr.getRetry()).thenReturn(true).thenReturn(false);

        when(client.isDone()).thenReturn(true);
        LinkedBlockingQueue<String> lbq = new LinkedBlockingQueue<>(10);
        tr.getHosebirdClient(lbq, config);
        verify(cb).name("Twitter Api Reader");
        verify(cb).hosts(Constants.STREAM_HOST);

        ArgumentCaptor<StreamingEndpoint> se = ArgumentCaptor.forClass(StreamingEndpoint.class);
        verify(cb).endpoint(se.capture());
        assertEquals("/1.1/statuses/filter.json?delimited=length&stall_warnings=true", se.getValue().getURI());

        // Unfortunately there's not a good way of asserting that the Authentication and Processor have
        // been created with the correct parameters.

        verify(cb).retries(10);
    }

    @Test
    public void should_return_correct_config_class() {
        TwitterApiReader gnipReader = new TwitterApiReader();
        assertEquals(TwitterApiReaderConfig.class, gnipReader.getConfigClass());
    }

    @Test
    public void should_create_correct_logger() {
        TwitterApiReader gnipReader = new TwitterApiReader();
        assertEquals(LoggerFactory.getLogger(TwitterApiReader.class), gnipReader.createLogger());
    }
}

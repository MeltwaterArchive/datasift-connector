package com.datasift.connector;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.datasift.connector.reader.Messages;
import com.datasift.connector.reader.Metrics;
import com.datasift.connector.reader.ReadAndSendPredicate;
import com.datasift.connector.reader.config.Config;
import com.datasift.connector.reader.config.GnipReaderConfig;
import com.datasift.connector.reader.config.HosebirdConfig;
import com.datasift.connector.reader.config.KafkaConfig;
import com.datasift.connector.reader.config.MetricsConfig;
import com.datasift.connector.reader.config.GnipConfig;
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


public class TestGnipReader {

    private BasicClient client = null;
    private ClientBuilder cb = null;
    private Logger logger = null;
    private GnipReaderConfig config = null;
    private ReadAndSendPredicate clientReadAndSendPredicate = null;

    @Before
    public void setup() {
        this.client = mock(BasicClient.class);
        this.cb = mock(ClientBuilder.class);
        when(cb.name("Gnip Reader")).thenReturn(cb);
        when(cb.hosts(Constants.ENTERPRISE_STREAM_HOST_v2)).thenReturn(cb);
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

        this.config = new GnipReaderConfig();
        this.config.gnip = new GnipConfig();
        this.config.hosebird = new HosebirdConfig();
        this.config.gnip.account = "ACCOUNT";
        this.config.gnip.product = "PRODUCT";
        this.config.gnip.label = "LABEL";
        this.config.gnip.username = "USERNAME";
        this.config.gnip.password = "PASSWORD";
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
        GnipReader tr = mock(GnipReader.class);
        when(tr.getHosebirdClient(any(LinkedBlockingQueue.class), any(GnipReaderConfig.class))).thenCallRealMethod();
        when(tr.getKafkaProducer(any(KafkaConfig.class))).thenReturn(producer);
        when(tr.getClientBuilder()).thenReturn(this.cb);
        doNothing().when(tr).readAndSend(any(LinkedBlockingQueue.class), anyString(), any(Producer.class), any(ReadAndSendPredicate.class));
        when(tr.parseConfigFile(anyString())).thenCallRealMethod();
        when(tr.getConfigClass()).thenCallRealMethod();

        Logger logger = mock(Logger.class);
        tr.setLogger(logger);
        String workingDir = System.getProperty("user.dir");
        GnipReaderConfig config = (GnipReaderConfig)tr.parseConfigFile(workingDir + "/src/test/resources/Valid.json");
        verify(logger, never()).error(anyString());
        assertEquals("ACCOUNT", config.gnip.account);
        assertEquals("LABEL", config.gnip.label);
        assertEquals("PRODUCT", config.gnip.product);
        assertEquals("USER", config.gnip.username);
        assertEquals("PASSWORD", config.gnip.password);
        assertEquals("http://localhost:5001", config.gnip.host);
        assertEquals(10, config.hosebird.retries);
        assertEquals(10000, config.hosebird.bufferSize);
        assertEquals(500, config.hosebird.bufferTimeout);
        assertEquals("twitter", config.kafka.topic);
        assertEquals("localhost:6667", config.kafka.servers);
        assertEquals(1001, config.kafka.retryBackoff);
        assertEquals(1002, config.kafka.reconnectBackoff);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getHosebirdCient_should_create_client_correctly() {
        Producer<String, String> producer = mock(Producer.class);
        GnipReader tr = mock(GnipReader.class);
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
        verify(cb).name("Gnip Reader");
        verify(cb).hosts(Constants.ENTERPRISE_STREAM_HOST_v2);

        ArgumentCaptor<StreamingEndpoint> se = ArgumentCaptor.forClass(StreamingEndpoint.class);
        verify(cb).endpoint(se.capture());
        assertEquals("/stream/PRODUCT/accounts/ACCOUNT/publishers/twitter/LABEL.json", se.getValue().getURI());

        // Unfortunately there's not a good way of asserting that the Authentication and Processor have
        // been created with the correct parameters.

        ArgumentCaptor<Authentication> a = ArgumentCaptor.forClass(Authentication.class);
        verify(cb).authentication(a.capture());
        BasicAuth expectedAuth = new BasicAuth("USERNAME", "PASSWORD");
        // assertEquals(expectedAuth, a.getValue());

        ArgumentCaptor<HosebirdMessageProcessor> hbmp = ArgumentCaptor.forClass(HosebirdMessageProcessor.class);
        verify(cb).processor(hbmp.capture());
        LineStringProcessor expectedProcessor = new LineStringProcessor(lbq);
        //assertEquals(expectedProcessor, hbmp.getValue());

        verify(cb).retries(10);
    }

    @Test
    public void should_return_correct_config_class() {
        GnipReader gnipReader = new GnipReader();
        assertEquals(GnipReaderConfig.class, gnipReader.getConfigClass());
    }

    @Test
    public void should_create_correct_logger() {
        GnipReader gnipReader = new GnipReader();
        assertEquals(LoggerFactory.getLogger(GnipReader.class), gnipReader.createLogger());
    }
}

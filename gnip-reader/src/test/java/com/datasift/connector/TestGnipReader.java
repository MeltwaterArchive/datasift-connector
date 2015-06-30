package com.datasift.connector;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.datasift.connector.reader.Messages;
import com.datasift.connector.reader.Metrics;
import com.datasift.connector.reader.ReadAndSendPredicate;
import com.datasift.connector.reader.config.Config;
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
    private Config config = null;
    private ReadAndSendPredicate clientReadAndSendPredicate = null;

    @Before
    public void setup() {
        this.client = mock(BasicClient.class);
        this.cb = mock(ClientBuilder.class);
        when(cb.name("Gnip Reader")).thenReturn(cb);
        when(cb.hosts(Constants.ENTERPRISE_STREAM_HOST)).thenReturn(cb);
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

        this.config = new Config();
        this.config.gnip = new GnipConfig();
        this.config.gnip.account = "ACCOUNT";
        this.config.gnip.product = "PRODUCT";
        this.config.gnip.label = "LABEL";
        this.config.gnip.username = "USERNAME";
        this.config.gnip.password = "PASSWORD";
        this.config.gnip.retries = 10;
        this.config.gnip.bufferSize = 10000;
        this.config.kafka = new KafkaConfig();
        this.config.kafka.topic = "Data";
        this.config.metrics = new MetricsConfig();
        this.config.metrics.host = "G_HOST";
        this.config.metrics.port = 1111;
        this.config.metrics.prefix = "G_PREFIX";
        this.config.metrics.reportingTime = 2;
    }

    @Test
    public void run_should_log_and_exit_if_arguments_empty() {
        reset(this.logger);
        GnipReader tr = mock(GnipReader.class);
        tr.setLogger(this.logger);
        String[] args = {};
        tr.run(args);
        verify(this.logger).error(Messages.EXIT_ARGUMENTS_EMPTY);
        verify(tr).exit(1);
    }

    @Test
    public void run_should_log_and_exit_if_cannot_parse_config_file() {
        reset(this.logger);
        GnipReader tr = mock(GnipReader.class);
        tr.setLogger(this.logger);
        String[] args = {"FILE"};
        tr.run(args);
        verify(this.logger).error(Messages.EXIT_CONFIG);
        verify(tr).exit(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void run_should_create_and_start_metrics_reporter() {
        GnipReader tr = mock(GnipReader.class);
        when(tr.parseConfigFile(anyString())).thenReturn(this.config);
        Metrics metrics = mock(Metrics.class);
        when(tr.getMetrics(any(StatsReporter.StatsTracker.class))).thenReturn(metrics);
        Client client = mock(Client.class);
        when(tr.getGnipClient(any(LinkedBlockingQueue.class), any(GnipConfig.class))).thenReturn(client);
        tr.setLogger(this.logger);
        StatsReporter stats = new StatsReporter();
        tr.metrics = new Metrics(stats.getStatsTracker());
        String[] args = {"FILE"};
        tr.run(args);

        verify(metrics).createAndStartStatsDReporter(any(MetricRegistry.class), eq("G_HOST"), eq(1111), eq("G_PREFIX"), eq(2));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void run_should_create_client_correctly_and_connect() {
        Producer<String, String> producer = mock(Producer.class);
        GnipReader tr = mock(GnipReader.class);
        reset(this.logger);
        tr.setLogger(this.logger);
        when(tr.getMetrics(any(StatsReporter.StatsTracker.class))).thenCallRealMethod();
        when(tr.getGnipClient(any(LinkedBlockingQueue.class), any(GnipConfig.class))).thenCallRealMethod();
        when(tr.getKafkaProducer(any(KafkaConfig.class))).thenReturn(producer);
        when(tr.getClientBuilder()).thenReturn(this.cb);
        when(tr.parseConfigFile(anyString())).thenReturn(config);
        when(tr.getRetry()).thenReturn(true).thenReturn(false);

        when(client.isDone()).thenReturn(true);
        LinkedBlockingQueue<String> lbq = new LinkedBlockingQueue<>(10);
        String[] args = {"1"};
        tr.run(args);

        verify(cb).name("Gnip Reader");
        verify(cb).hosts(Constants.ENTERPRISE_STREAM_HOST);

        ArgumentCaptor<StreamingEndpoint> se = ArgumentCaptor.forClass(StreamingEndpoint.class);
        verify(cb).endpoint(se.capture());
        assertEquals("/accounts/ACCOUNT/publishers/twitter/streams/PRODUCT/LABEL.json", se.getValue().getURI());

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

        verify(client).connect();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void run_should_take_from_buffer_until_client_is_done() {
        Producer<String, String> producer = mock(Producer.class);
        GnipReader tr = mock(GnipReader.class);
        reset(this.logger);
        tr.setLogger(this.logger);
        when(tr.getMetrics(any(StatsReporter.StatsTracker.class))).thenCallRealMethod();
        when(tr.getGnipClient(any(LinkedBlockingQueue.class), any(GnipConfig.class))).thenCallRealMethod();
        when(tr.getKafkaProducer(any(KafkaConfig.class))).thenReturn(producer);
        when(tr.getClientBuilder()).thenReturn(this.cb);
        doCallRealMethod().when(tr).readAndSend(any(LinkedBlockingQueue.class), anyString(), any(Producer.class), any(ReadAndSendPredicate.class));
        LinkedBlockingQueue<String> buffer = new LinkedBlockingQueue<>(2);
        buffer.add("1");
        buffer.add("2");
        when(tr.getBufferQueue(anyInt())).thenReturn(buffer);
        when(tr.parseConfigFile(anyString())).thenReturn(config);
        when(client.isDone()).thenReturn(false, false, true);
        when(tr.getRetry()).thenReturn(true).thenReturn(false);

        String[] args = {"1"};
        tr.run(args);

        assertEquals(0, buffer.size());
        verify(client, times(3)).isDone();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void run_should_send_each_message_to_kafka() {
        Producer<String, String> producer = mock(Producer.class);
        GnipReader tr = mock(GnipReader.class);
        reset(this.logger);
        tr.setLogger(this.logger);
        when(tr.getMetrics(any(StatsReporter.StatsTracker.class))).thenCallRealMethod();
        when(tr.getGnipClient(any(LinkedBlockingQueue.class), any(GnipConfig.class))).thenCallRealMethod();
        when(tr.getKafkaProducer(any(KafkaConfig.class))).thenReturn(producer);
        when(tr.getClientBuilder()).thenReturn(this.cb);
        doCallRealMethod().when(tr).readAndSend(any(LinkedBlockingQueue.class), anyString(), any(Producer.class), any(ReadAndSendPredicate.class));
        LinkedBlockingQueue<String> buffer = new LinkedBlockingQueue<>(2);
        buffer.add("1");
        buffer.add("2");
        when(tr.getBufferQueue(anyInt())).thenReturn(buffer);
        when(tr.parseConfigFile(anyString())).thenReturn(config);

        when(client.isDone()).thenReturn(false, false, true);
        when(tr.getRetry()).thenReturn(true).thenReturn(false);

        String[] args = {"1"};
        tr.run(args);

        assertEquals(0, buffer.size());
        verify(client, times(3)).isDone();
        ArgumentCaptor<ProducerRecord> pr = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(producer, times(2)).send(pr.capture(), any(Callback.class));
        List<ProducerRecord> values = pr.getAllValues();
        assertEquals("Data", values.get(0).topic());
        assertEquals("0", values.get(0).key());
        assertEquals("1", values.get(0).value());
        assertEquals("Data", values.get(1).topic());
        assertEquals("0", values.get(1).key());
        assertEquals("2", values.get(1).value());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void run_should_log_if_kafka_send_fails() {
        reset(this.logger);
        Producer<String, String> producer = mock(Producer.class);

        GnipReader tr = mock(GnipReader.class);
        tr.setLogger(this.logger);
        when(tr.getGnipClient(any(LinkedBlockingQueue.class), any(GnipConfig.class))).thenCallRealMethod();

        when(tr.getMetrics(any(StatsReporter.StatsTracker.class))).thenCallRealMethod();
        when(tr.getKafkaProducer(any(KafkaConfig.class))).thenReturn(producer);
        when(tr.getClientBuilder()).thenReturn(this.cb);
        doCallRealMethod().when(tr).readAndSend(any(LinkedBlockingQueue.class), anyString(), any(Producer.class), any(ReadAndSendPredicate.class));
        LinkedBlockingQueue<String> buffer = new LinkedBlockingQueue<>(2);
        buffer.add("1");
        when(tr.getBufferQueue(anyInt())).thenReturn(buffer);
        when(tr.parseConfigFile(anyString())).thenReturn(config);

        when(client.isDone()).thenReturn(false, true);
        when(tr.getRetry()).thenReturn(true).thenReturn(false);

        String[] args = {"1"};
        tr.run(args);

        ArgumentCaptor<Callback> c = ArgumentCaptor.forClass(Callback.class);
        verify(producer, times(1)).send(any(ProducerRecord.class), c.capture());
        c.getValue().onCompletion(null, new Exception("ERROR"));
        verify(this.logger).error("Exception sending message to Kafka: {}", "ERROR", "1");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void run_should_not_log_if_kafka_send_succeeds() {
        reset(this.logger);
        Producer<String, String> producer = mock(Producer.class);

        GnipReader tr = mock(GnipReader.class);
        tr.setLogger(this.logger);
        when(tr.getMetrics(any(StatsReporter.StatsTracker.class))).thenCallRealMethod();
        when(tr.getGnipClient(any(LinkedBlockingQueue.class), any(GnipConfig.class))).thenCallRealMethod();
        when(tr.getKafkaProducer(any(KafkaConfig.class))).thenReturn(producer);
        when(tr.getClientBuilder()).thenReturn(this.cb);
        doCallRealMethod().when(tr).readAndSend(any(LinkedBlockingQueue.class), anyString(), any(Producer.class), any(ReadAndSendPredicate.class));
        LinkedBlockingQueue<String> buffer = new LinkedBlockingQueue<>(2);
        buffer.add("1");
        when(tr.getBufferQueue(anyInt())).thenReturn(buffer);
        when(tr.parseConfigFile(anyString())).thenReturn(config);

        when(client.isDone()).thenReturn(false, true);
        when(tr.getRetry()).thenReturn(true).thenReturn(false);

        String[] args = {"1"};
        tr.run(args);

        ArgumentCaptor<Callback> c = ArgumentCaptor.forClass(Callback.class);
        verify(producer, times(1)).send(any(ProducerRecord.class), c.capture());
        c.getValue().onCompletion(null, null);
        verify(this.logger, never()).error(anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void run_should_log_if_taking_from_the_buffer_throws() {
        try {
            reset(this.logger);
            Producer<String, String> producer = mock(Producer.class);
            GnipReader tr = mock(GnipReader.class);
            tr.setLogger(this.logger);
            when(tr.getMetrics(any(StatsReporter.StatsTracker.class))).thenCallRealMethod();
            when(tr.getGnipClient(any(LinkedBlockingQueue.class), any(GnipConfig.class))).thenCallRealMethod();
            when(tr.getKafkaProducer(any(KafkaConfig.class))).thenReturn(producer);
            when(tr.getClientBuilder()).thenReturn(this.cb);
            doCallRealMethod().when(tr).readAndSend(any(LinkedBlockingQueue.class), anyString(), any(Producer.class), any(ReadAndSendPredicate.class));
            when(client.isDone()).thenReturn(false, true);
            LinkedBlockingQueue<String> buffer = mock(LinkedBlockingQueue.class);
            when(buffer.take()).thenThrow(new InterruptedException("ERROR"));
            when(tr.getBufferQueue(anyInt())).thenReturn(buffer);
            when(tr.parseConfigFile(anyString())).thenReturn(config);
            when(tr.getRetry()).thenReturn(true).thenReturn(false);

            String[] args = {"1"};
            tr.run(args);

            verify(this.logger).error("Error reading from buffer queue: {}", "ERROR");

        } catch(Exception e) {
            assert(false);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void run_should_retry_if_readandsend_ends() {
        GnipReader tr = mock(GnipReader.class);
        tr.setLogger(this.logger);
        when(tr.getMetrics(any(StatsReporter.StatsTracker.class))).thenCallRealMethod();
        when(tr.parseConfigFile(anyString())).thenReturn(config);
        Client client = mock(Client.class);
        when(tr.getGnipClient(any(LinkedBlockingQueue.class), any(GnipConfig.class))).thenReturn(client);
        Producer<String, String> producer = mock(Producer.class);
        when(tr.getKafkaProducer(any(KafkaConfig.class))).thenReturn(producer);
        when(tr.getRetry()).thenReturn(true).thenReturn(true).thenReturn(false);

        String[] args = {"1"};
        tr.run(args);

        verify(tr, times(2)).readAndSend(any(LinkedBlockingQueue.class), anyString(), any(Producer.class), any(ReadAndSendPredicate.class));
        verify(tr, times(2)).logClientExitReason(any(Client.class));
        verify(client, times(2)).connect();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void run_should_mark_metric_if_readandsend_ends() {
        GnipReader tr = mock(GnipReader.class);
        tr.setLogger(this.logger);
        when(tr.parseConfigFile(anyString())).thenReturn(config);
        when(tr.getMetrics(any(StatsReporter.StatsTracker.class))).thenCallRealMethod();
        Client client = mock(Client.class);
        when(tr.getGnipClient(any(LinkedBlockingQueue.class), any(GnipConfig.class))).thenReturn(client);
        Producer<String, String> producer = mock(Producer.class);
        when(tr.getKafkaProducer(any(KafkaConfig.class))).thenReturn(producer);
        when(tr.getRetry()).thenReturn(true).thenReturn(true).thenReturn(false);

        String[] args = {"1"};
        tr.run(args);

        assertEquals(2, tr.metrics.disconnected.getCount());
    }

    // TODO what is the behaviour of the hbc client when the buffer is full?
    // See AbstractProcessor.java process()
    // Offers the message to the queue with a default timeout of 500ms,
    // can pass this in to the LineStringProcessor constructor.
    // If the offer fails the message is dropped and the stats counter
    // is incremented, see ClientBase.java processConnectionData()

    @Test
    public void getkafkaproducer_should_create_producer_correctly() {
        GnipReader tr = new GnipReader();
        Producer<String, String> producer = tr.getKafkaProducer(new KafkaConfig());
        assertNotNull(producer);
        // No way to check the properties
    }

    @Test
    public void getclientbuilder_should_create_builder_correctly() {
        GnipReader tr = new GnipReader();
        ClientBuilder cb = tr.getClientBuilder();
        assertNotNull(cb);
    }

    @Test
    public void getbufferqueue_should_create_queue_correctly() {
        GnipReader tr = new GnipReader();
        LinkedBlockingQueue<String> q = tr.getBufferQueue(10);
        assertEquals(10, q.remainingCapacity());
    }

    @Ignore("Integration test, requires a zookeeper and Kafka. Un-Ignore to test manually")
    @Test
    public void send_should_send_messsage_to_kafka() {

        int port = 9092;

        // Create the buffer, put the message in there and then read
        when(client.isDone()).thenReturn(false, true);
        int numberOfMessages = 1000;
        LinkedBlockingQueue<String> buffer = new LinkedBlockingQueue<>(numberOfMessages);

        for(int i = 0; i < numberOfMessages; ++i) {
            buffer.add("MESSAGE" + i);
        }

        GnipReader tr = new GnipReader();
        Producer<String, String> producer = tr.getKafkaProducer(new KafkaConfig());
        tr.readAndSend(buffer, "TOPIC", producer, this.clientReadAndSendPredicate);
        producer.close();

        System.out.println("Messages sent");

        // Consume one message from Kafka:
        SimpleConsumer consumer = new SimpleConsumer("localhost", port, 10000, 1024000, "CLIENT");

        FetchRequest req = new FetchRequestBuilder()
                .clientId("CLIENT")
                .addFetch("TOPIC", 0, 0, 100000)
                .build();

        FetchResponse fetchResponse = consumer.fetch(req);
        int count = 0;

        for(MessageAndOffset msg : fetchResponse.messageSet("TOPIC", 0)) {
            Message m = msg.message();
            ByteBuffer bb = m.payload();
            CharBuffer cb = StandardCharsets.UTF_8.decode(bb);
            assertTrue(cb.toString().startsWith("MESSAGE"));
            count++;
        }

        // TODO either clear the topic at the start of the test or
        //      check how many message are in the topic.
        assertTrue(count > 1);
        consumer.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_log_error_if_config_not_JSON() {
        Producer<String, String> producer = mock(Producer.class);
        GnipReader tr = mock(GnipReader.class);
        when(tr.getGnipClient(any(LinkedBlockingQueue.class), any(GnipConfig.class))).thenCallRealMethod();
        when(tr.getKafkaProducer(any(KafkaConfig.class))).thenReturn(producer);
        when(tr.getClientBuilder()).thenReturn(this.cb);
        doNothing().when(tr).readAndSend(any(LinkedBlockingQueue.class), anyString(), any(Producer.class), any(ReadAndSendPredicate.class));
        when(tr.parseConfigFile(anyString())).thenCallRealMethod();

        Logger logger = mock(Logger.class);
        tr.setLogger(logger);
        String workingDir = System.getProperty("user.dir");
        Config config = tr.parseConfigFile(workingDir + "/src/test/resources/NotJson.txt");
        verify(logger).error(Messages.CONFIG_NOT_JSON);
        assertNull(config);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_log_error_if_config_missing_items() {
        Producer<String, String> producer = mock(Producer.class);
        GnipReader tr = mock(GnipReader.class);
        when(tr.getGnipClient(any(LinkedBlockingQueue.class), any(GnipConfig.class))).thenCallRealMethod();
        when(tr.getKafkaProducer(any(KafkaConfig.class))).thenReturn(producer);
        when(tr.getClientBuilder()).thenReturn(this.cb);
        doNothing().when(tr).readAndSend(any(LinkedBlockingQueue.class), anyString(), any(Producer.class), any(ReadAndSendPredicate.class));
        when(tr.parseConfigFile(anyString())).thenCallRealMethod();

        Logger logger = mock(Logger.class);
        tr.setLogger(logger);
        String workingDir = System.getProperty("user.dir");
        Config config = tr.parseConfigFile(workingDir + "/src/test/resources/MissingItems.json");
        ArgumentCaptor<String> constraints = ArgumentCaptor.forClass(String.class);
        verify(logger).error(
                eq("{} {}"),
                eq(Messages.CONFIG_MISSING_ITEMS),
                constraints.capture());

        // The order of the messages can be different every time so just check
        // that the key words are present.
        assertTrue(constraints.getValue().contains("metrics"));
        assertTrue(constraints.getValue().contains("kafka"));
        assertTrue(constraints.getValue().contains("gnip"));
        assertNull(config);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_log_error_if_config_not_readable() {
        Producer<String, String> producer = mock(Producer.class);
        GnipReader tr = mock(GnipReader.class);
        when(tr.getGnipClient(any(LinkedBlockingQueue.class), any(GnipConfig.class))).thenCallRealMethod();
        when(tr.getKafkaProducer(any(KafkaConfig.class))).thenReturn(producer);
        when(tr.getClientBuilder()).thenReturn(this.cb);
        doNothing().when(tr).readAndSend(any(LinkedBlockingQueue.class), anyString(), any(Producer.class), any(ReadAndSendPredicate.class));
        when(tr.parseConfigFile(anyString())).thenCallRealMethod();

        Logger logger = mock(Logger.class);
        tr.setLogger(logger);
        Config config = tr.parseConfigFile("INVALID");
        verify(logger).error(Messages.CONFIG_NOT_READABLE);
        assertNull(config);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_parse_valid_config() {
        Producer<String, String> producer = mock(Producer.class);
        GnipReader tr = mock(GnipReader.class);
        when(tr.getGnipClient(any(LinkedBlockingQueue.class), any(GnipConfig.class))).thenCallRealMethod();
        when(tr.getKafkaProducer(any(KafkaConfig.class))).thenReturn(producer);
        when(tr.getClientBuilder()).thenReturn(this.cb);
        doNothing().when(tr).readAndSend(any(LinkedBlockingQueue.class), anyString(), any(Producer.class), any(ReadAndSendPredicate.class));
        when(tr.parseConfigFile(anyString())).thenCallRealMethod();

        Logger logger = mock(Logger.class);
        tr.setLogger(logger);
        String workingDir = System.getProperty("user.dir");
        Config config = tr.parseConfigFile(workingDir + "/src/test/resources/Valid.json");
        verify(logger, never()).error(anyString());
        assertEquals("ACCOUNT", config.gnip.account);
        assertEquals("LABEL", config.gnip.label);
        assertEquals("PRODUCT", config.gnip.product);
        assertEquals("USER", config.gnip.username);
        assertEquals("PASSWORD", config.gnip.password);
        assertEquals("http://localhost:5001", config.gnip.host);
        assertEquals(10, config.gnip.retries);
        assertEquals(10000, config.gnip.bufferSize);
        assertEquals(500, config.gnip.bufferTimeout);
        assertEquals("twitter-gnip", config.kafka.topic);
        assertEquals("localhost:9092", config.kafka.servers);
        assertEquals(1001, config.kafka.retryBackoff);
        assertEquals(1002, config.kafka.reconnectBackoff);
    }

    @Test
    public void should_log_reason_that_client_stopped() {
        Event event = new Event(EventType.STOPPED_BY_ERROR, new Exception("EXCEPTION"));
        when(this.client.getExitEvent()).thenReturn(event);

        GnipReader tr = new GnipReader();
        reset(this.logger);
        tr.setLogger(this.logger);

        tr.logClientExitReason(this.client);

        verify(this.logger).error(
                "Gnip client stopped: {} {}",
                new Object[]{
                        "STOPPED_BY_ERROR",
                        "EXCEPTION"
                });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shutdownhook_should_send_all_messages_in_buffer_to_kafka() {
        GnipReader tr = mock(GnipReader.class);
        tr.setLogger(this.logger);
        StatsReporter stats = new StatsReporter();
        tr.metrics = new Metrics(stats.getStatsTracker());
        tr.metrics.shutdown = mock(Meter.class);
        tr.metrics.read = mock(Meter.class);
        doCallRealMethod().when(tr).addShutdownHook(any(Client.class), any(LinkedBlockingQueue.class), anyString(), any(Producer.class));
        doCallRealMethod().when(tr).readAndSend(any(LinkedBlockingQueue.class), anyString(), any(Producer.class), any(ReadAndSendPredicate.class));

        LinkedBlockingQueue buffer = new LinkedBlockingQueue(5);
        buffer.add("1");
        buffer.add("2");
        buffer.add("3");
        buffer.add("4");
        buffer.add("5");
        KafkaProducer<String, String> producer = mock(KafkaProducer.class);
        tr.addShutdownHook(client, buffer, "TOPIC", producer);

        ArgumentCaptor<Thread> threadCaptor = ArgumentCaptor.forClass(Thread.class);
        verify(tr).runtimeAddShutdownHook(threadCaptor.capture());
        Thread thread = threadCaptor.getValue();
        thread.run();

        try {
            thread.join();
        } catch(Exception e) {
            assertNull(e);
        }

        assertEquals(0, buffer.size());
        verify(producer, times(5)).send(any(ProducerRecord.class), any(Callback.class));
        verify(client).stop();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shutdownhook_should_end_if_exception_thrown() {
        GnipReader tr = mock(GnipReader.class);
        tr.setLogger(this.logger);
        StatsReporter stats = new StatsReporter();
        tr.metrics = new Metrics(stats.getStatsTracker());
        tr.metrics.shutdown = mock(Meter.class);
        doCallRealMethod().when(tr).addShutdownHook(any(Client.class), any(LinkedBlockingQueue.class), anyString(), any(Producer.class));
        doThrow(new RuntimeException("ERROR")).when(tr).readAndSend(any(LinkedBlockingQueue.class), anyString(), any(Producer.class), any(ReadAndSendPredicate.class));

        LinkedBlockingQueue buffer = new LinkedBlockingQueue(1);
        buffer.add("1");
        KafkaProducer<String, String> producer = mock(KafkaProducer.class);
        tr.addShutdownHook(client, buffer, "TOPIC", producer);

        ArgumentCaptor<Thread> threadCaptor = ArgumentCaptor.forClass(Thread.class);
        verify(tr).runtimeAddShutdownHook(threadCaptor.capture());
        Thread thread = threadCaptor.getValue();

        try {
            thread.run();
        } catch(Exception e) {
            assertEquals("ERROR", e.getMessage());
        }

        verify(producer, times(0)).send(any(ProducerRecord.class), any(Callback.class));
        assertEquals(1, buffer.size());
    }

    @Test
    public void retry_default_should_be_true() {
        GnipReader tr = new GnipReader();
        assertTrue(tr.getRetry());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void can_add_shutdownhook() {
        try {
            Class clazz = Class.forName("java.lang.ApplicationShutdownHooks");
            Field field = clazz.getDeclaredField("hooks");
            field.setAccessible(true);
            IdentityHashMap<Thread, Thread> hooks = (IdentityHashMap<Thread, Thread>)field.get(null);
            int initialHooksNumber = hooks.size();

            GnipReader tr = new GnipReader();
            tr.runtimeAddShutdownHook(new Thread());

            IdentityHashMap<Thread, Thread> hooks2 = (IdentityHashMap<Thread, Thread>)field.get(null);
            assertEquals(initialHooksNumber + 1, hooks2.size());
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shutdownhook_should_set_retry_to_false() {
        GnipReader tr = mock(GnipReader.class);
        tr.setLogger(this.logger);
        StatsReporter stats = new StatsReporter();
        tr.metrics = new Metrics(stats.getStatsTracker());
        tr.metrics.shutdown = mock(Meter.class);
        doCallRealMethod().when(tr).addShutdownHook(any(Client.class), any(LinkedBlockingQueue.class), anyString(), any(Producer.class));

        LinkedBlockingQueue buffer = new LinkedBlockingQueue(1);
        buffer.add("1");
        KafkaProducer<String, String> producer = mock(KafkaProducer.class);
        tr.addShutdownHook(client, buffer, "TOPIC", producer);

        ArgumentCaptor<Thread> threadCaptor = ArgumentCaptor.forClass(Thread.class);
        verify(tr).runtimeAddShutdownHook(threadCaptor.capture());
        Thread thread = threadCaptor.getValue();

        thread.run();

        try {
            thread.join();
        } catch(Exception e) {
            assertNull(e);
        }

        verify(tr).setRetry(false);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shutdownhook_should_close_kafka_producer() {
        GnipReader tr = mock(GnipReader.class);
        tr.setLogger(this.logger);
        StatsReporter stats = new StatsReporter();
        tr.metrics = new Metrics(stats.getStatsTracker());
        tr.metrics.shutdown = mock(Meter.class);
        doCallRealMethod().when(tr).addShutdownHook(any(Client.class), any(LinkedBlockingQueue.class), anyString(), any(Producer.class));

        LinkedBlockingQueue buffer = new LinkedBlockingQueue(1);
        buffer.add("1");
        KafkaProducer<String, String> producer = mock(KafkaProducer.class);
        tr.addShutdownHook(client, buffer, "TOPIC", producer);

        ArgumentCaptor<Thread> threadCaptor = ArgumentCaptor.forClass(Thread.class);
        verify(tr).runtimeAddShutdownHook(threadCaptor.capture());
        Thread thread = threadCaptor.getValue();

        thread.run();

        try {
            thread.join();
        } catch(Exception e) {
            assertNull(e);
        }

        verify(producer).close();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shutdownhook_should_mark_metric() {
        GnipReader tr = mock(GnipReader.class);
        tr.setLogger(this.logger);
        Meter meter = mock(Meter.class);
        StatsReporter stats = new StatsReporter();
        tr.metrics = new Metrics(stats.getStatsTracker());
        tr.metrics.shutdown = meter;
        doCallRealMethod().when(tr).addShutdownHook(any(Client.class), any(LinkedBlockingQueue.class), anyString(), any(Producer.class));

        LinkedBlockingQueue buffer = new LinkedBlockingQueue(1);
        buffer.add("1");
        KafkaProducer<String, String> producer = mock(KafkaProducer.class);
        tr.addShutdownHook(client, buffer, "TOPIC", producer);

        ArgumentCaptor<Thread> threadCaptor = ArgumentCaptor.forClass(Thread.class);
        verify(tr).runtimeAddShutdownHook(threadCaptor.capture());
        Thread thread = threadCaptor.getValue();

        thread.run();

        try {
            thread.join();
        } catch(Exception e) {
            assertNull(e);
        }

        verify(meter).mark();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_add_metric_when_message_is_read_from_buffer() {
        GnipReader tr = mock(GnipReader.class);
        tr.setLogger(this.logger);
        Meter meter = mock(Meter.class);
        StatsReporter stats = new StatsReporter();
        tr.metrics = new Metrics(stats.getStatsTracker());
        tr.metrics.read =  meter;
        doCallRealMethod().when(tr).readAndSend(any(LinkedBlockingQueue.class), anyString(), any(Producer.class), any(ReadAndSendPredicate.class));
        LinkedBlockingQueue<String> buffer = new LinkedBlockingQueue<>(2);
        buffer.add("1");
        Producer producer = mock(KafkaProducer.class);
        ReadAndSendPredicate predicate = mock(ReadAndSendPredicate.class);
        when(predicate.process()).thenReturn(true).thenReturn(false);

        tr.readAndSend(buffer, "TOPIC", producer, predicate);

        verify(meter).mark();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_add_metric_when_message_is_sent_to_kafka() {
        GnipReader tr = mock(GnipReader.class);
        tr.setLogger(this.logger);
        Meter meter = mock(Meter.class);
        StatsReporter stats = new StatsReporter();
        tr.metrics = new Metrics(stats.getStatsTracker());
        tr.metrics.read = mock(Meter.class);
        tr.metrics.sent =  meter;
        doCallRealMethod().when(tr).readAndSend(any(LinkedBlockingQueue.class), anyString(), any(Producer.class), any(ReadAndSendPredicate.class));
        LinkedBlockingQueue<String> buffer = new LinkedBlockingQueue<>(2);
        buffer.add("1");
        Producer producer = mock(KafkaProducer.class);
        ReadAndSendPredicate predicate = mock(ReadAndSendPredicate.class);
        when(predicate.process()).thenReturn(true).thenReturn(false);

        tr.readAndSend(buffer, "TOPIC", producer, predicate);

        ArgumentCaptor<Callback> c = ArgumentCaptor.forClass(Callback.class);
        verify(producer, times(1)).send(any(ProducerRecord.class), c.capture());
        c.getValue().onCompletion(null, null);

        verify(meter).mark();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_add_metric_when_errors_sending_message_to_kafka() {
        GnipReader tr = mock(GnipReader.class);
        tr.setLogger(this.logger);
        Meter meter = mock(Meter.class);
        StatsReporter stats = new StatsReporter();
        tr.metrics = new Metrics(stats.getStatsTracker());
        tr.metrics.read = mock(Meter.class);
        tr.metrics.sent =  mock(Meter.class);
        tr.metrics.sendError = meter;
        doCallRealMethod().when(tr).readAndSend(any(LinkedBlockingQueue.class), anyString(), any(Producer.class), any(ReadAndSendPredicate.class));
        LinkedBlockingQueue<String> buffer = new LinkedBlockingQueue<>(2);
        buffer.add("1");
        Producer producer = mock(KafkaProducer.class);
        ReadAndSendPredicate predicate = mock(ReadAndSendPredicate.class);
        when(predicate.process()).thenReturn(true).thenReturn(false);

        tr.readAndSend(buffer, "TOPIC", producer, predicate);

        ArgumentCaptor<Callback> c = ArgumentCaptor.forClass(Callback.class);
        verify(producer, times(1)).send(any(ProducerRecord.class), c.capture());
        c.getValue().onCompletion(null, new Exception("ERROR"));

        verify(meter).mark();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_add_metric_when_read_from_buffer_errors() {
        try {
            GnipReader tr = mock(GnipReader.class);
            tr.setLogger(this.logger);
            Meter meter = mock(Meter.class);
            StatsReporter stats = new StatsReporter();
            tr.metrics = new Metrics(stats.getStatsTracker());
            tr.metrics.readError = meter;
            doCallRealMethod().when(tr).readAndSend(any(LinkedBlockingQueue.class), anyString(), any(Producer.class), any(ReadAndSendPredicate.class));
            LinkedBlockingQueue<String> buffer = mock(LinkedBlockingQueue.class);
            when(buffer.take()).thenThrow(new InterruptedException("ERROR"));
            Producer producer = mock(KafkaProducer.class);
            ReadAndSendPredicate predicate = mock(ReadAndSendPredicate.class);
            when(predicate.process()).thenReturn(true).thenReturn(false);

            tr.readAndSend(buffer, "TOPIC", producer, predicate);

            verify(meter).mark();
        } catch(Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void should_create_gauges_for_gnip_client_stats() {
        StatsReporter stats = new StatsReporter();
        stats.incrNum200s();
        stats.incrNum400s();
        stats.incrNum500s();
        stats.incrNumMessages();
        stats.incrNumDisconnects();
        stats.incrNumConnects();
        stats.incrNumConnectionFailures();
        stats.incrNumClientEventsDropped();
        stats.incrNumMessagesDropped();

        Metrics m = new Metrics(stats.getStatsTracker());
        assertEquals(1, m.num200s.getValue().intValue());
        assertEquals(1, m.num400s.getValue().intValue());
        assertEquals(1, m.num500s.getValue().intValue());
        assertEquals(1, m.messages.getValue().longValue());
        assertEquals(1, m.disconnects.getValue().intValue());
        assertEquals(1, m.connections.getValue().intValue());
        assertEquals(1, m.connectionFailures.getValue().intValue());
        assertEquals(1, m.clientEventsDropped.getValue().longValue());
        assertEquals(1, m.messagesDropped.getValue().longValue());
    }

    @Test
    public void can_set_retry() {
        GnipReader tr = new GnipReader();
        tr.setRetry(true);

        try {
            Field f = tr.getClass().getDeclaredField("retry");
            f.setAccessible(true);
            AtomicBoolean retry = (AtomicBoolean) f.get(tr);
            assertEquals(true, retry.get());
        } catch(Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void should_set_kafka_config_on_producer() {
        GnipReader tr = new GnipReader();
        KafkaConfig config = new KafkaConfig();
        try {
            Producer<String, String> producerWithConfig = tr.getKafkaProducer(config);
            Field f1 = producerWithConfig.getClass().getDeclaredField("producerConfig");
            f1.setAccessible(true);
            ProducerConfig pc = (ProducerConfig)f1.get(producerWithConfig);
            assertEquals(1000, pc.getLong(ProducerConfig.RETRY_BACKOFF_MS_CONFIG));

            Field f2 = producerWithConfig.getClass().getDeclaredField("producerConfig");
            f2.setAccessible(true);
            ProducerConfig pc2 = (ProducerConfig)f2.get(producerWithConfig);
            assertEquals(1000, pc2.getLong(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG));
        } catch (Exception e) {
            assertNull(e);
        }
    }
}

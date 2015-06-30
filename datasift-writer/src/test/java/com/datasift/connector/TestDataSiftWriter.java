package com.datasift.connector;

import com.codahale.metrics.MetricRegistry;
import com.datasift.connector.writer.BulkManager;
import com.datasift.connector.writer.Messages;
import com.datasift.connector.writer.Metrics;
import com.datasift.connector.writer.SimpleConsumerManager;
import com.datasift.connector.writer.config.Config;
import com.datasift.connector.writer.config.KafkaConfig;
import com.datasift.connector.writer.config.DataSiftConfig;
import com.datasift.connector.writer.config.MetricsConfig;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

public class TestDataSiftWriter {

    private final static String CONFIG_VALID = "/src/test/resources/config.json";
    private final static String CONFIG_NOT_JSON =
            "/src/test/resources/config_not_json.txt";
    private final static String CONFIG_MISSING_ITEM = "/src/test/resources/config_missing_item.json";
    private final static String CONFIG_MISSING = "/src/test/resources/does_not_exist.json";

    private Logger logger = null;
    private Config config = null;

    @Before
    public void setup() {
        this.logger = mock(Logger.class);

        this.config = new Config();
        this.config.datasift = new DataSiftConfig();
        this.config.datasift.baseURL = "https://in.datasift.com";
        this.config.datasift.port = 443;
        this.config.datasift.username = "user";
        this.config.datasift.apiKey = "584b068700b0385493686a8d3f95696ac";
        this.config.datasift.sourceID = "a981db85e4bd452e80b4ccfc508e0c63";
        this.config.kafka = new KafkaConfig();
        this.config.kafka.topic = "twitter-gnip";
        this.config.metrics = new MetricsConfig();
        this.config.metrics.host = "G_HOST";
        this.config.metrics.port = 1111;
        this.config.metrics.prefix = "G_PREFIX";
        this.config.metrics.reportingTime = 2;
    }

    @Test
    public void run_should_log_and_exit_if_arguments_empty() {
        reset(this.logger);
        DataSiftWriter dw = mock(DataSiftWriter.class);
        dw.setLogger(this.logger);
        String[] args = {};
        dw.run(args);
        verify(this.logger).error(Messages.EXIT_ARGUMENTS_EMPTY);
        verify(dw).exit(1);
    }

    @Test
    public void run_should_log_and_exit_if_cannot_parse_config_file() {
        reset(this.logger);
        DataSiftWriter dw = mock(DataSiftWriter.class);
        dw.setLogger(this.logger);
        String[] args = {"FILE"};
        dw.run(args);
        verify(this.logger).error(Messages.EXIT_CONFIG);
        verify(dw).exit(1);
    }

    @Test public void parsing_valid_config_returns_valid_object() {
        DataSiftWriter dw = mock(DataSiftWriter.class);
        when(dw.parseConfigFile(anyString())).thenCallRealMethod();

        Logger logger = mock(Logger.class);
        dw.setLogger(logger);
        String workingDir = System.getProperty("user.dir");
        Config config = dw.parseConfigFile(workingDir + CONFIG_VALID);

        verify(logger, never()).error(anyString());
        assertEquals("localhost:2181", config.zookeeper.socket);
        assertEquals("twitter-gnip", config.kafka.topic);
        assertEquals("localhost", config.kafka.broker);
        assertEquals("https://in.datasift.com", config.datasift.baseURL);
        assertEquals((Integer)443, config.datasift.port);
        assertEquals("user", config.datasift.username);
        assertEquals("584b068700b0385493686a8d3f95696ac", config.datasift.apiKey);
        assertEquals("a981db85e4bd452e80b4ccfc508e0c63", config.datasift.sourceID);
        assertEquals("localhost", config.metrics.host);
        assertEquals(8125, config.metrics.port);
        assertEquals("datasift.writer", config.metrics.prefix);
        assertEquals(1, config.metrics.reportingTime);
    }

    @Test public void parsing_invalid_json_returns_error() {
        DataSiftWriter dw = mock(DataSiftWriter.class);
        when(dw.parseConfigFile(anyString())).thenCallRealMethod();

        Logger logger = mock(Logger.class);
        dw.setLogger(logger);
        String workingDir = System.getProperty("user.dir");
        Config config = dw.parseConfigFile(workingDir + CONFIG_NOT_JSON);
        verify(logger).error(Messages.CONFIG_NOT_JSON);
        assertNull(config);
    }

    @Test public void parsing_config_with_missing_item_returns_error() {
        DataSiftWriter dw = mock(DataSiftWriter.class);
        when(dw.parseConfigFile(anyString())).thenCallRealMethod();

        Logger logger = mock(Logger.class);
        dw.setLogger(logger);
        String workingDir = System.getProperty("user.dir");
        Config config = dw.parseConfigFile(workingDir + CONFIG_MISSING_ITEM);
        ArgumentCaptor<String> constraints = ArgumentCaptor.forClass(String.class);
        verify(logger).error(
                eq("{} {}"),
                eq(Messages.CONFIG_MISSING_ITEMS),
                constraints.capture());
        assertTrue(constraints.getValue().contains("metrics"));
        assertTrue(constraints.getValue().contains("kafka"));
        assertTrue(constraints.getValue().contains("datasift"));
        assertTrue(constraints.getValue().contains("zookeeper"));
        assertNull(config);
    }

    @Test public void parsing_missing_config_file_returns_error() {
        DataSiftWriter dw = mock(DataSiftWriter.class);
        when(dw.parseConfigFile(anyString())).thenCallRealMethod();

        Logger logger = mock(Logger.class);
        dw.setLogger(logger);
        Config config = dw.parseConfigFile(CONFIG_MISSING);
        verify(logger).error(Messages.CONFIG_NOT_READABLE);
        assertNull(config);
    }


    @SuppressWarnings("unchecked")
    @Test public void run_should_create_and_start_metrics_reporter() {
        DataSiftWriter dw = mock(DataSiftWriter.class);
        when(dw.parseConfigFile(anyString())).thenReturn(this.config);
        dw.numConsumers = 1;

        Logger logger = mock(Logger.class);
        dw.setLogger(logger);
        Metrics metrics = mock(Metrics.class);
        dw.metrics = metrics;
        String[] args = {"FILE"};
        dw.run(args);

        verify(metrics).createAndStartStatsDReporter(any(MetricRegistry.class), eq("G_HOST"), eq(1111), eq("G_PREFIX"), eq(2));
    }

    @Ignore
    @SuppressWarnings("unchecked")
    @Test public void run_should_mark_process_metric() {
        DataSiftWriter dw = mock(DataSiftWriter.class);
        when(dw.parseConfigFile(anyString())).thenReturn(this.config);
        dw.numConsumers = 1;
        Logger logger = mock(Logger.class);
        dw.setLogger(logger);
        Metrics metrics = new Metrics();
        dw.metrics = metrics;
        when(dw.shouldProcess()).thenReturn(true).thenReturn(false);
        //ConsumerQueue cq = mock(ConsumerQueue.class);
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(1);
        queue.add("1");
        //when(cq.getQueue()).thenReturn(queue);
        //dw.consumerQueue = cq;
        when(dw.shouldProcess()).thenReturn(true).thenReturn(false);

        String[] args = {"FILE"};
        dw.run(args);

        assertEquals(1, metrics.sendAttempt.getCount());
    }

    @Test public void should_set_should_read_default_to_true() {
        DataSiftWriter dsw = new DataSiftWriter();
        assertTrue(dsw.shouldRead());
    }

    @Test public void should_set_should_process_default_to_true() {
        DataSiftWriter dsw = new DataSiftWriter();
        assertTrue(dsw.shouldProcess());
    }

    @Test public void should_add_shutdown_hook() {
        DataSiftWriter dsw = new DataSiftWriter();
        DataSiftWriter dswS = spy(dsw);
        dswS.addShutdownHook(1);

        verify(dswS).runtimeAddShutdownHook(any(Thread.class));
    }

    @Test public void should_log_status() {
        DataSiftWriter dsw = new DataSiftWriter();
        Logger log = mock(Logger.class);
        dsw.setLogger(log);
        dsw.logStatistics();
        verify(log).info(anyString());
    }

    @Test public void should_submit_bulk_manager_to_executor() {
        DataSiftWriter dsw = mock(DataSiftWriter.class);
        doCallRealMethod().when(dsw).setupBulk(any(Config.class), any(SimpleConsumerManager.class), any(Metrics.class), any(Logger.class));
        ExecutorService executor = mock(ExecutorService.class);
        SimpleConsumerManager consumer = mock(SimpleConsumerManager.class);
        when(dsw.newSingleThreadExecutor()).thenReturn(executor);
        when(dsw.parseConfigFile(anyString())).thenCallRealMethod();
        String workingDir = System.getProperty("user.dir");

        Config config = dsw.parseConfigFile(workingDir + CONFIG_VALID);

        Metrics metrics = mock(Metrics.class);
        Logger log = mock(Logger.class);

        dsw.setupBulk(config, consumer, metrics, log);

        ArgumentCaptor<BulkManager> arg = ArgumentCaptor.forClass(BulkManager.class);
        verify(executor).submit(arg.capture());
        assertEquals(BulkManager.class, arg.getValue().getClass());
    }
}

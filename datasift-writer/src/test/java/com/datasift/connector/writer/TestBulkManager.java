package com.datasift.connector.writer;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.datasift.connector.writer.config.DataSiftConfig;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class TestBulkManager {

    // This starts the mock server up for every test
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(18089);

    @Test
    public void should_construct_authorization_token_correctly() {
        DataSiftConfig config = new DataSiftConfig();
        config.username = "USER";
        config.apiKey = "KEY";
        BulkManager bm = new BulkManager(config, null, null, null);
        assertEquals("USER:KEY", bm.getAuthorizationToken(config));
    }

    @Test
    public void should_get_uri_correctly() {
        DataSiftConfig config = new DataSiftConfig();
        config.baseURL = "http://in.example.com/";
        config.sourceID = "SOURCEID";
        config.port = 42;
        config.username = "USER";
        config.apiKey = "APIKEY";
        BulkManager b = new BulkManager(config, null, null, null);

        try {
            assertEquals("http://in.example.com:42/SOURCEID", b.getUri().toASCIIString());
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void post_should_post_to_ingestion_endpoint() throws URISyntaxException, InterruptedException {
        Thread.sleep(1000); // Give WireMock time
        stubFor(post(urlEqualTo("/SOURCEID"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Ingestion-Data-RateLimit-Reset-Ttl", "1000")
                        .withBody("{\"accepted\":1,\"total_message_bytes\":2}")));

        Logger log = mock(Logger.class);
        DataSiftConfig config = new DataSiftConfig();
        Metrics metrics = new Metrics();
        config.baseURL = "http://localhost/";
        config.sourceID = "SOURCEID";
        config.port = 18089;
        config.username = "USER";
        config.apiKey = "APIKEY";

        BulkManager bm = new BulkManager(config, null, null, metrics);
        try {
            HttpResponse response = bm.post("{}");
            String body = EntityUtils.toString(response.getEntity());
            assertEquals("{\"accepted\":1,\"total_message_bytes\":2}", body);
        } catch (Exception e) {
            e.printStackTrace();
            assertNull(e);
        }
    }

    @Test
    public void send_should_reset_backoff_on_success() throws Exception {
        Thread.sleep(1000); // Give WireMock time
        stubFor(post(urlEqualTo("/SOURCEID"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Ingestion-Data-RateLimit-Reset-Ttl", "1000")
                        .withBody("{\"accepted\":1,\"total_message_bytes\":2}")));

        Logger log = mock(Logger.class);
        Metrics metrics = new Metrics();
        Backoff backoff = new Backoff(null, log, metrics);
        SimpleConsumerManager consumer = mock(SimpleConsumerManager.class);

        DataSiftConfig config = new DataSiftConfig();
        config.baseURL = "http://localhost/";
        config.sourceID = "SOURCEID";
        config.port = 18089;
        config.username = "USER";
        config.apiKey = "APIKEY";

        BulkManager bm = new BulkManager(config, consumer, backoff, metrics);
        bm.setLogger(log);
        bm.send(new BulkReadValues(0, "{}"));

        verify(log).debug("Reset backoff time");
    }

    @Test
    public void send_should_wait_until_if_413() throws Exception {
        Thread.sleep(1000); // Give WireMock time
        stubFor(post(urlEqualTo("/SOURCEID"))
                .willReturn(aResponse()
                        .withStatus(413)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Ingestion-Data-RateLimit-Reset-Ttl", "1000")
                        .withBody("{\"error\":\"This request's size exceeds the available data limit\"}")));

        Logger log = mock(Logger.class);
        Metrics metrics = new Metrics();
        Backoff backoff = mock(Backoff.class);
        SimpleConsumerManager consumer = mock(SimpleConsumerManager.class);

        DataSiftConfig config = new DataSiftConfig();
        config.baseURL = "http://localhost/";
        config.sourceID = "SOURCEID";
        config.port = 18089;
        config.username = "USER";
        config.apiKey = "APIKEY";

        BulkManager bm = new BulkManager(config, consumer, backoff, metrics);
        bm.setLogger(log);
        bm.send(new BulkReadValues(0, "{}"));

        verify(backoff).waitUntil(1000);
        ArgumentCaptor<Date> arg = ArgumentCaptor.forClass(Date.class);
        verify(log).info(eq("Rate limited, waiting until limits reset at {}"), arg.capture());
        assertTrue(arg.getValue().getTime() > 1000);
    }

    @Test
    public void send_should_backoff_exponentially_if_status_code_400_plus() throws InterruptedException {
        Thread.sleep(1000); // Give WireMock time
        stubFor(post(urlEqualTo("/SOURCEID"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Ingestion-Data-RateLimit-Reset-Ttl", "1000")
                        .withBody("{\"error\":\"This request's size exceeds the available data limit\"}")));

        Logger log = mock(Logger.class);
        Metrics metrics = new Metrics();
        Backoff backoff = mock(Backoff.class);
        SimpleConsumerManager consumer = mock(SimpleConsumerManager.class);

        DataSiftConfig config = new DataSiftConfig();
        config.baseURL = "http://localhost/";
        config.sourceID = "SOURCEID";
        config.port = 18089;
        config.username = "USER";
        config.apiKey = "APIKEY";

        BulkManager bm = new BulkManager(config, consumer, backoff, metrics);
        bm.setLogger(log);
        bm.send(new BulkReadValues(0, "{}"));

        verify(backoff).exponentialBackoff();
        verify(log).error("Error code returned from ingestion endpoint, status = {}", 503);
    }

    @Test
    public void send_should_backoff_linearly_on_exception() throws InterruptedException {
        DataSiftConfig config = new DataSiftConfig();
        config.baseURL = "INVALID";
        config.sourceID = "SOURCEID";
        config.port = 18089;
        config.username = "USER";
        config.apiKey = "APIKEY";

        Logger log = mock(Logger.class);
        Metrics metrics = new Metrics();
        Backoff backoff = mock(Backoff.class);
        SimpleConsumerManager consumer = mock(SimpleConsumerManager.class);
        BulkManager bm = new BulkManager(config, consumer, backoff, metrics);
        bm.setLogger(log);
        bm.send(new BulkReadValues(0, "{}"));

        verify(backoff).linearBackoff();
        verify(log).error(eq("Could not connect to ingestion endpoint"), any(Exception.class));
    }

    @Test
    public void should_not_send_if_buffer_empty() throws Exception {
        BulkManager bm = new BulkManager(null, null, null, null);
        bm.send(new BulkReadValues(0, new StringBuilder().toString()));
        // Will throw a NullPointerException if allowed to carry on
    }


    @Test
    public void should_mark_sent_attempt_metric_before_post() throws Exception {
        Thread.sleep(1000); // Give WireMock time
        stubFor(post(urlEqualTo("/SOURCEID"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Ingestion-Data-RateLimit-Reset-Ttl", "1000")
                        .withBody("{\"accepted\":1,\"total_message_bytes\":2}")));

        Backoff backoff = mock(Backoff.class);
        DataSiftConfig config = new DataSiftConfig();
        config.baseURL = "http://localhost/";
        config.sourceID = "SOURCEID";
        config.port = 18089;
        config.username = "USER";
        config.apiKey = "APIKEY";

        Metrics metrics = new Metrics();
        Logger log = mock(Logger.class);
        BulkManager bm = new BulkManager(config, null, backoff, metrics);
        bm.setLogger(log);

        bm.post("{}");

        assertEquals(1, metrics.sendAttempt.getCount());
    }

    @Test
    public void send_should_time_post() throws Exception {
        Thread.sleep(1000); // Give WireMock time
        stubFor(post(urlEqualTo("/SOURCEID"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Ingestion-Data-RateLimit-Reset-Ttl", "1000")
                        .withBody("{\"accepted\":1,\"total_message_bytes\":2}")));

        Backoff backoff = mock(Backoff.class);
        DataSiftConfig config = new DataSiftConfig();
        config.baseURL = "http://localhost/";
        config.sourceID = "SOURCEID";
        config.port = 18089;
        config.username = "USER";
        config.apiKey = "APIKEY";

        SimpleConsumerManager consumer = mock(SimpleConsumerManager.class);
        Meter meter = mock(Meter.class);
        Timer.Context context = mock(Timer.Context.class);
        Timer timer = mock(Timer.class);
        when(timer.time()).thenReturn(context);
        MetricRegistry registry = mock(MetricRegistry.class);
        when(registry.timer("bulk-post-time")).thenReturn(timer);
        when(registry.meter(anyString())).thenReturn(meter);
        Metrics metrics = new Metrics(registry);
        Logger log = mock(Logger.class);
        BulkManager bm = new BulkManager(config, consumer, backoff, metrics);
        bm.setLogger(log);

        bm.send(new BulkReadValues(0, "{}"));

        verify(context).stop();
    }

    @Test
    public void should_mark_success_metrics_on_200() throws Exception {
        Thread.sleep(1000); // Give WireMock time
        stubFor(post(urlEqualTo("/SOURCEID"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Ingestion-Data-RateLimit-Reset-Ttl", "1000")
                        .withBody("{\"accepted\":1,\"total_message_bytes\":2}")));

        Logger log = mock(Logger.class);
        ConsumerData consumerData = mock(ConsumerData.class);
        when(consumerData.getMessage()).thenReturn("{}");
        SimpleConsumerManager consumer = mock(SimpleConsumerManager.class);
        when(consumer.readItem()).thenReturn(consumerData);
        when(consumer.commit()).thenReturn(true);
        DataSiftConfig config = new DataSiftConfig();
        config.baseURL = "http://localhost/";
        config.sourceID = "SOURCEID";
        config.port = 18089;
        config.username = "USER";
        config.apiKey = "APIKEY";
        Metrics metrics = new Metrics();
        Backoff backoff = mock(Backoff.class);
        BulkManager bm = new BulkManager(config, consumer, backoff, metrics);

        bm.send(new BulkReadValues(3, "{}"));

        assertEquals(1, metrics.sendSuccess.getCount());
        assertEquals(3, metrics.sentItems.getCount());
    }

    @Test
    public void should_mark_sent_error_on_400() throws Exception {
        Thread.sleep(1000); // Give WireMock time
        stubFor(post(urlEqualTo("/SOURCEID"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Ingestion-Data-RateLimit-Reset-Ttl", "1000")
                        .withBody("{\"accepted\":1,\"total_message_bytes\":2}")));

        Logger log = mock(Logger.class);
        Backoff backoff = mock(Backoff.class);
        ConsumerData consumerData = mock(ConsumerData.class);
        when(consumerData.getMessage()).thenReturn("{}");
        SimpleConsumerManager consumer = mock(SimpleConsumerManager.class);
        when(consumer.readItem()).thenReturn(consumerData);
        DataSiftConfig config = new DataSiftConfig();
        config.baseURL = "http://localhost/";
        config.sourceID = "SOURCEID";
        config.port = 18089;
        config.username = "USER";
        config.apiKey = "APIKEY";
        Metrics metrics = new Metrics();
        BulkManager bm = new BulkManager(config, consumer, backoff, metrics);
        bm.setLogger(log);

        bm.send(new BulkReadValues(0, "{}"));

        assertEquals(1, metrics.sendError.getCount());
    }

    @Test
    public void should_mark_sent_error_on_413() throws Exception {
        Thread.sleep(1000); // Give WireMock time
        stubFor(post(urlEqualTo("/SOURCEID"))
                .willReturn(aResponse()
                        .withStatus(413)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Ingestion-Data-RateLimit-Reset-Ttl", "1000")
                        .withBody("{\"accepted\":1,\"total_message_bytes\":2}")));

        Logger log = mock(Logger.class);
        Backoff backoff = mock(Backoff.class);
        ConsumerData consumerData = mock(ConsumerData.class);
        when(consumerData.getMessage()).thenReturn("{}");
        SimpleConsumerManager consumer = mock(SimpleConsumerManager.class);
        when(consumer.readItem()).thenReturn(consumerData);
        DataSiftConfig config = new DataSiftConfig();
        config.baseURL = "http://localhost/";
        config.sourceID = "SOURCEID";
        config.port = 18089;
        config.username = "USER";
        config.apiKey = "APIKEY";
        Metrics metrics = new Metrics();
        BulkManager bm = new BulkManager(config, consumer, backoff, metrics);

        bm.send(new BulkReadValues(0, "{}"));

        assertEquals(1, metrics.sendRateLimit.getCount());
    }

    @Test
    public void should_mark_sent_exception_on_exception() throws Exception {
        Thread.sleep(1000); // Give WireMock time
        stubFor(post(urlEqualTo("/SOURCEID"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Ingestion-Data-RateLimit-Reset-Ttl", "1000")
                        .withBody("{\"accepted\":1,\"total_message_bytes\":2}")));

        Logger log = mock(Logger.class);
        Backoff backoff = mock(Backoff.class);
        SimpleConsumerManager consumer = mock(SimpleConsumerManager.class);
        Metrics metrics = new Metrics();
        DataSiftConfig config = new DataSiftConfig();
        config.baseURL = "INVALID";
        config.sourceID = "SOURCEID";
        config.port = 18089;
        config.username = "USER";
        config.apiKey = "APIKEY";
        BulkManager bm = new BulkManager(config, consumer, backoff, metrics);
        bm.setLogger(log);

        bm.send(new BulkReadValues(0, "{}"));

        assertEquals(1, metrics.sendException.getCount());
    }

    @Test
    public void shutdown_should_set_running_to_false() {
        BulkManager bm = new BulkManager(null, null, null, null);
        bm.shutdown();
        assertFalse(bm.isRunning());
    }

    @Test
    public void run_should_not_start_when_shutdown() throws InterruptedException {
        BulkManager bm = new BulkManager(null, null, null, null);
        bm.shutdown();
        bm.run();
    }

    @Test
    public void should_set_config() {
        DataSiftConfig config = new DataSiftConfig();
        config.baseURL = "http://localhost/";
        config.sourceID = "SOURCEID";
        config.port = 18089;
        config.username = "USER";
        config.apiKey = "APIKEY";

        BulkManager bm = new BulkManager(config, null, null, null);
        assertEquals(config, bm.getConfig());
    }

    @Test
    public void getDataFromKafka_should_return_just_item_if_buffer_empty() {
        SimpleConsumerManager scm = mock(SimpleConsumerManager.class);
        ConsumerData cd = mock(ConsumerData.class);
        when(cd.getMessage()).thenReturn("{\"DATA\":\"1\"}");
        when(scm.readItem()).thenReturn(cd);
        Metrics metrics = new Metrics();
        BulkManager bm = new BulkManager(null, scm, null, metrics);
        StringBuilder sb = new StringBuilder();
        assertTrue(bm.getDataFromKafka(sb));
        assertEquals("{\"DATA\":\"1\"}", sb.toString());
        assertEquals(1, metrics.readKafkaItemFromConsumer.getCount());
    }

    @Test
    public void getDataFromKafka_should_concatenate_item_to_not_empty_stringbuilder() {
        SimpleConsumerManager scm = mock(SimpleConsumerManager.class);
        ConsumerData cd = mock(ConsumerData.class);
        when(cd.getMessage()).thenReturn("{\"DATA\":\"1\"}");
        when(scm.readItem()).thenReturn(cd);
        Metrics metrics = new Metrics();
        BulkManager bm = new BulkManager(null, scm, null, metrics);
        StringBuilder sb = new StringBuilder();
        sb.append("EXISTING");
        assertTrue(bm.getDataFromKafka(sb));
        assertEquals("EXISTING\r\n{\"DATA\":\"1\"}", sb.toString());
    }

    @Test
    public void getDataFromKafka_should_drop_message_that_is_not_a_json_object() {
        SimpleConsumerManager scm = mock(SimpleConsumerManager.class);
        ConsumerData cd = mock(ConsumerData.class);
        when(cd.getMessage()).thenReturn("DATA");
        when(scm.readItem()).thenReturn(cd);
        Metrics metrics = new Metrics();
        BulkManager bm = new BulkManager(null, scm, null, metrics);
        StringBuilder sb = new StringBuilder();
        sb.append("EXISTING");
        assertFalse(bm.getDataFromKafka(sb));
        assertEquals("EXISTING", sb.toString());
    }

    @Test
    public void getDataFromKafka_should_not_concatenate_if_no_item_returned() {
        SimpleConsumerManager scm = mock(SimpleConsumerManager.class);
        Metrics metrics = new Metrics();
        BulkManager bm = new BulkManager(null, scm, null, metrics);
        StringBuilder sb = new StringBuilder();
        sb.append("EXISTING");
        assertFalse(bm.getDataFromKafka(sb));
        assertEquals("EXISTING", sb.toString());
        assertEquals(0, metrics.readKafkaItemFromConsumer.getCount());
    }

    @Test
    public void read_should_read_for_configured_interval() {
        SimpleConsumerManager scm = mock(SimpleConsumerManager.class);
        Metrics metrics = new Metrics();
        ConsumerData cd = mock(ConsumerData.class);
        when(cd.getMessage()).thenReturn("{\"ONE\":\"1\"}").thenReturn("{\"TWO\":\"2\"}");
        when(scm.readItem()).thenReturn(cd).thenReturn(cd).thenReturn(null);
        Logger log = mock(Logger.class);
        DataSiftConfig config = new DataSiftConfig();
        config.bulkInterval = 1000;
        config.bulkItems = 999999999;
        config.bulkSize = 999999999;

        BulkManager bm = new BulkManager(config, scm, null, metrics);
        bm.setLogger(log);

        long before = System.nanoTime();
        String data = bm.read().getData();
        long after = System.nanoTime();

        assertTrue(after - before >= TimeUnit.MILLISECONDS.toNanos(config.bulkInterval));
        assertEquals("{\"ONE\":\"1\"}\r\n{\"TWO\":\"2\"}", data);
    }

    @Test
    public void read_should_read_for_configured_items() {
        SimpleConsumerManager scm = mock(SimpleConsumerManager.class);
        ConsumerData cd = mock(ConsumerData.class);
        when(cd.getMessage()).thenReturn("{\"ONE\":\"1\"}").thenReturn("{\"TWO\":\"2\"}");
        when(scm.readItem()).thenReturn(cd).thenReturn(cd).thenReturn(null);
        Logger log = mock(Logger.class);
        Metrics metrics = new Metrics();
        DataSiftConfig config = new DataSiftConfig();
        config.bulkInterval = 999999999;
        config.bulkSize = 99999999;
        config.bulkItems = 2;

        BulkManager bm = new BulkManager(config, scm, null, metrics);
        bm.setLogger(log);

        String data = bm.read().getData();

        assertEquals("{\"ONE\":\"1\"}\r\n{\"TWO\":\"2\"}", data);
        assertEquals(1, metrics.readKafkaItemsFromConsumer.getCount());
        assertEquals(2, metrics.readKafkaItemFromConsumer.getCount());
    }

    @Test
    public void read_should_read_for_configured_size() {
        SimpleConsumerManager scm = mock(SimpleConsumerManager.class);
        ConsumerData cd = mock(ConsumerData.class);
        when(cd.getMessage()).thenReturn("{\"ONE\":\"1\"}").thenReturn("{\"TWO\":\"2\"}");
        when(scm.readItem()).thenReturn(cd).thenReturn(cd).thenReturn(null);
        Logger log = mock(Logger.class);
        Metrics metrics = new Metrics();
        DataSiftConfig config = new DataSiftConfig();
        config.bulkInterval = 999999999;
        config.bulkSize = 14;
        config.bulkItems = 99999999;

        BulkManager bm = new BulkManager(config, scm, null, metrics);
        bm.setLogger(log);

        String data = bm.read().getData();

        assertEquals("{\"ONE\":\"1\"}\r\n{\"TWO\":\"2\"}", data);
        assertEquals(1, metrics.readKafkaItemsFromConsumer.getCount());
        assertEquals(2, metrics.readKafkaItemFromConsumer.getCount());
    }

    @Test
    public void read_should_log_items_read() {
        SimpleConsumerManager scm = mock(SimpleConsumerManager.class);
        ConsumerData cd = mock(ConsumerData.class);
        when(cd.getMessage()).thenReturn("{\"ONE\":\"1\"}").thenReturn("{\"TWO\":\"2\"}");
        when(scm.readItem()).thenReturn(cd).thenReturn(cd).thenReturn(null);
        Logger log = mock(Logger.class);
        Metrics metrics = new Metrics();
        DataSiftConfig config = new DataSiftConfig();
        config.bulkInterval = 999999999;
        config.bulkItems = 3;

        BulkManager bm = new BulkManager(config, scm, null, metrics);
        bm.setLogger(log);

        bm.read();

        verify(log).debug("Read {} items from Kafka", 2);
    }

    @Ignore("Integration test for debugging")
    @Test
    public void run_should_read_from_kafka_and_post_to_endpoint() throws IOException {
        // TODO allow isRunning to be changed in test
        stubFor(post(urlEqualTo("/SOURCEID"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Ingestion-Data-RateLimit-Reset-Ttl", "1000")
                        .withBody("{\"accepted\":1,\"total_message_bytes\":2}")));

        Logger log = mock(Logger.class);
        Metrics metrics = new Metrics();
        Sleeper sleeper = new Sleeper();
        Backoff backoff = new Backoff(sleeper, log, metrics);
        DataSiftConfig config = new DataSiftConfig();
        config.bulkInterval = 1000;
        config.bulkSize = 150000;
        config.bulkItems = 10000;
        config.baseURL = "http://localhost/";
        config.sourceID = "SOURCEID";
        config.port = 443;
        config.username = "USERID";
        config.apiKey = "APIKEY";

        String data = "{\"a\":\"b\"}";
        ConsumerData cd = new ConsumerData(1, data);
        SimpleConsumerManager scm = mock(SimpleConsumerManager.class);
        when(scm.readItem()).thenReturn(cd);
        BulkManager bm = new BulkManager(config, scm, backoff, metrics);
        bm.run();

        // Don't want to ever have this test running, it's for debugging
        assertTrue(false);
    }
}

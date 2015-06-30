package com.datasift.connector.writer;

import com.codahale.metrics.Timer;
import com.datasift.connector.writer.config.DataSiftConfig;
import com.google.common.annotations.VisibleForTesting;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Sends bulk uploads to the DataSift Ingestion endpoint.
 */
public class BulkManager implements Runnable {

    /**
     * The backoff logic.
     */
    private final Backoff backoff;

    /**
     * The Metrics container.
     */
    private Metrics metrics;

    /**
     * Whether the manager is running.
     */
    private volatile Boolean running = true;

    /**
     * Gets whether the manager is running.
     * @return whether the manager is running
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected Boolean isRunning() {
        return this.running;
    }

    /**
     * The simple consumer manager for Kafka.
     */
    private SimpleConsumerManager simpleConsumerManager;

    /**
     * The logger to send messages to.
     */
    private static Logger log =
            LoggerFactory.getLogger(BulkManager.class);

    /**
     * Set the logger.
     * @param logger the logger to use
     */
    public final void setLogger(final Logger logger) {
        log = logger;
    }

    /**
     * DataSift connection configuration.
     */
    private DataSiftConfig config;

    /**
     * Constructor.
     * @param config configuration for DataSift HTTP connection
     * @param simpleConsumerManager the simple consumer manager for Kafka
     * @param backoff the object to manage backoffs
     * @param metrics the metrics object
     */
    public BulkManager(final DataSiftConfig config,
                       final SimpleConsumerManager simpleConsumerManager,
                       final Backoff backoff,
                       final Metrics metrics) {
        this.config = config;
        this.simpleConsumerManager = simpleConsumerManager;
        this.backoff = backoff;
        this.metrics = metrics;
    }

    /**
     * Shuts down the manager.
     */
    public final void shutdown() {
        log.info("Shutting down HTTP bulk upload manager");
        this.running = false;
    }

    /**
     * Gets the configuration.
     * @return the configuration
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected DataSiftConfig getConfig() {
        return this.config;
    }

    /**
     * Parses DataSift configuration and sets up connection parameters.
     * @return the URI for the ingestion endpoint
     * @throws Exception on issues constructing URI from base URL & port
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected URI getUri() throws Exception {
        String baseURL = config.baseURL;
        if (!baseURL.endsWith("/")) {
            baseURL += "/";
        }

        String url = baseURL + config.sourceID;
        URI uri = UriBuilder
                .fromUri(url)
                .port(config.port).build();

        if (uri == null) {
            String msg = "DataSift configuration base URL "
                    + "and/or port syntax invalid";
            log.error(msg);
            throw new Exception("uri-syntax");
        }

        return uri;
    }

    /**
     * Gets the authorization token to use in the headers.
     * @param config the configuration to parse
     * @return the authorization token
     */
    @SuppressWarnings("checkstyle:designforextension")
    protected String getAuthorizationToken(final DataSiftConfig config) {
       return config.username + ":" + config.apiKey;
    }

    /**
     * Runs the manager.
     */
    public final void run() {
        while (isRunning()) {
            try {
                String data = read();
                send(data);
            } catch (InterruptedException e) {
                log.error("Run loop interrupted ", e);
            }
        }
    }


    /**
     * Reads data from Kafka. Will read for the configured time or the
     * configured bulk size, whichever is first.
     * @return the updated StringBuilder
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected String read() {
        final long start = System.nanoTime();
        StringBuilder buffer = new StringBuilder();
        int loop = 0;
        int read = 0;

        do {
            if (getDataFromKafka(buffer)) {
                read++;
            }
        } while (System.nanoTime() - start < TimeUnit.MILLISECONDS.toNanos(config.bulkInterval)
                && ++loop < config.bulkSize);

        log.debug("Read {} items from Kafka", read);
        metrics.readKafkaItemsFromConsumer.mark();

        return buffer.toString();
    }

    /**
     *
     * Gets the data from Kafka.
     * @param buffer the items for the current bulk upload
     * @return the updated StringBuilder
     */
    @VisibleForTesting
    @SuppressWarnings({"checkstyle:designforextension"})
    protected boolean getDataFromKafka(final StringBuilder buffer) {

        ConsumerData cd = simpleConsumerManager.readItem();
        if (cd == null) {
            return false;
        }

        if (buffer.length() > 0) {
            buffer.append("\r\n");
        }

        metrics.readKafkaItemFromConsumer.mark();
        buffer.append(cd.getMessage());
        return true;
    }

    /**
     * Reads from Kafka and sends to the DataSift ingestion endpoint.
     * Deals with back-offs if unsuccessful.
     * @param data the data to post
     * @throws InterruptedException if the waits are interrupted
     */
    @SuppressWarnings("checkstyle:designforextension")
    protected void send(final String data) throws InterruptedException {
        try {
            if (data.equals("")) {
                return;
            }

            final Timer.Context context = metrics.bulkPostTime.time();
            HttpResponse response = post(data);
            context.stop();

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                simpleConsumerManager.commit();
                String body = EntityUtils.toString((response.getEntity()));
                metrics.sendSuccess.mark();
                backoff.reset();
                log.trace("Data successfully sent to ingestion endpoint: {}", body);
                log.debug("Data successfully sent to ingestion endpoint: {}", body.hashCode());
            } else if (statusCode == HttpStatus.SC_REQUEST_TOO_LONG) {
                long ttl = Long.parseLong(
                            response
                             .getFirstHeader("X-Ingestion-Data-RateLimit-Reset-Ttl")
                             .getValue());
                log.info("Rate limited, waiting until limits reset at {}", new Date(ttl));
                metrics.sendRateLimit.mark();
                backoff.waitUntil(ttl);
            } else if (statusCode >= HttpStatus.SC_BAD_REQUEST) {
                log.error("Error code returned from ingestion endpoint, status = {}", statusCode);
                metrics.sendError.mark();
                backoff.exponentialBackoff();
            }
        } catch (Exception e) {
            log.error("Could not connect to ingestion endpoint", e);
            metrics.sendException.mark();
            backoff.linearBackoff();
        }
    }

    /**
     * Post the data to the DataSift Ingestion endpoint.
     * @param data the data to send
     * @return the http response
     * @throws Exception if the uri is invalid or the request fails
     */
    @SuppressWarnings("checkstyle:designforextension")
    public HttpResponse post(final String data) throws Exception {
        URI uri = getUri();
        String authToken = getAuthorizationToken(config);

        metrics.sendAttempt.mark();
        log.trace("Posting to ingestion endpoint {}", data);

        return Request
                    .Post(uri)
                    .addHeader("Auth", authToken)
                    .bodyString(data, ContentType.create("application/json"))
                    .execute()
                    .returnResponse();
    }
}

package com.datasift.connector;

import com.datasift.connector.reader.Messages;
import com.datasift.connector.reader.Metrics;
import com.datasift.connector.reader.ReadAndSendPredicate;
import com.datasift.connector.reader.config.Config;
import com.datasift.connector.reader.config.KafkaConfig;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.StatsReporter;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.httpclient.BasicClient;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reads data from the Hosebird client and sends to Kafka.
 */
public abstract class HosebirdReader {

    /**
     * The metrics.
     */
    @VisibleForTesting
    protected Metrics metrics = null;

    /**
     * The logger to send messages to.
     */
    private Logger log;

    /**
     * Gets the logger.
     * @return the logger
     */
    protected final Logger getLogger() {
        return this.log;
    }

    /**
     * Whether the reader should retry if the Hosebird client exits.
     */
    private AtomicBoolean retry = new AtomicBoolean(true);

    /**
     * Set whether the reader should retry if the Hosebird client exits.
     * Set to false in the shutdown hook.
     * @param shouldRetry whether to retry
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected void setRetry(final boolean shouldRetry) {
        this.retry.set(shouldRetry);
    }

    /**
     * Get whether the reader should retry if the Hosebird client exits.
     * @return whether to retry
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected boolean getRetry() {
        return this.retry.get();
    }

    /**
     * Set the logger. Used by tests.
     * @param logger the logger to use
     */
    @VisibleForTesting
    protected final void setLogger(final Logger logger) {
        this.log = logger;
    }

    /**
     * Create the logger.
     * @return the created Logger
     */
    @VisibleForTesting
    protected abstract Logger createLogger();

    /**
     * The constructor.
     */
    public HosebirdReader() {
        this.setLogger(this.createLogger());
    }

    /**
     * Connects to Hosebird then loops until the client says it is done, reading
     * the message from the buffer and sending it onto the onward queue.
     * @param args command line arguments
     */
    public final void run(final String[] args) {

        // The configuration file is always required as an argument
        if (args.length == 0) {
            log.error(Messages.EXIT_ARGUMENTS_EMPTY);
            exit(1);
            return;
        }

        // Parse the config file provided
        Config config = parseConfigFile(args[0]);
        if (config == null) {
            log.error(Messages.EXIT_CONFIG);
            exit(1);
            return;
        }

        // Create the Hosebird client and buffer queue
        LinkedBlockingQueue<String> buffer =
                getBufferQueue(config.hosebird.bufferSize);

        final Client client = getHosebirdClient(
                buffer,
                config);

        // Initialise the metrics
        this.metrics = getMetrics(client.getStatsTracker());
        this.metrics.createAndStartStatsDReporter(
                metrics.getRegistry(),
                config.metrics.host,
                config.metrics.port,
                config.metrics.prefix,
                config.metrics.reportingTime);

        // Client to send messages to the onward queue
        Producer<String, String> producer = getKafkaProducer(config.kafka);

        // Ensure that on shutdown we tidy up gracefully
        addShutdownHook(
                client,
                buffer,
                config.kafka.topic,
                producer);

        log.info("Starting external retry loop");
        while (this.getRetry()) {
            // If the Hosebird client cannot connect it will retry with
            // a linear back-off.
            client.connect();
            readAndSend(
                    buffer,
                    config.kafka.topic,
                    producer,
                    new ReadAndSendPredicate() {
                @Override
                public boolean process() {
                    return !client.isDone();
                }
            });

            // If we get here it means that the Hosebird client has said
            // that it has finished any retries and stopped. Log the reason,
            // dump the stats and try connecting again. This will restart
            // the  back-off of the Hosebird client.
            logClientExitReason(client);
            metrics.disconnected.mark();
            // TODO send the statistics in client.getStatsTracker() to statsd
        }
    }

    /**
     * Add a hook that gracefully deals with shutdown.
     * Drain the buffer queue and send to Kafka before exit.
     * @param client the gnip client
     * @param buffer the queue which the Hosebird client reads into
     * @param onwardQueueTopic the name of the Kafka topic to send to
     * @param producer the Kafka Producer used to send the message
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected void addShutdownHook(
        final Client client,
        final LinkedBlockingQueue<String> buffer,
        final String onwardQueueTopic,
        final Producer<String, String> producer) {

        this.runtimeAddShutdownHook(new Thread() {
            public void run() {
                log.info("Shutdown cleanup");
                metrics.shutdown.mark();

                // Let the run loop know we're stopping
                setRetry(false);

                // Gracefully stop the Hosebird client
                client.stop();

                log.info("{} messages left in queue", buffer.size());

                // Drain the buffer of any unsent messages
                readAndSend(
                        buffer,
                        onwardQueueTopic,
                        producer,
                        new ReadAndSendPredicate() {
                    @Override
                    public boolean process() {
                        return buffer.size() != 0;
                    }
                });

                log.info("Finished draining buffer");

                // Gracefully stop the Kafka producer
                producer.close();

                log.info("Shutdown hook finished");
            }
        });
    }

    /**
     * Call System.exit with the supplied code.
     * Allows testing of code calling exit.
     * @param code the exit code to use
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected void exit(final int code) {
        log.warn("Exiting the service with code {}", code);
        System.exit(code);
    }

    /**
     * Returns a DataSift Writer configuration object parsed from JSON file.
     * @param jsonFile String file path of the JSON file to parse
     * @return a Config object representing JSON data provided
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected Config parseConfigFile(final String jsonFile) {

        log.info("Parsing configuration file");

        try {
            ObjectMapper mapper = new ObjectMapper();
            Config config =  (Config) mapper.readValue(new File(jsonFile), getConfigClass());
            Set<ConstraintViolation<Config>> problems =
                    Validation.buildDefaultValidatorFactory()
                              .getValidator()
                              .validate(config);

            if (problems.size() == 0) {
                return config;
            }

            String constraintErrors = "";
            for (ConstraintViolation<Config> constraint: problems) {
                constraintErrors += String.format(
                        "\n%s %s",
                        constraint.getPropertyPath(),
                        constraint.getMessage());
            }

            log.error("{} {}", Messages.CONFIG_MISSING_ITEMS, constraintErrors);
            return null;

        } catch (JsonMappingException | JsonParseException e) {
            log.error(Messages.CONFIG_NOT_JSON);
        } catch (IOException e) {
            log.error(Messages.CONFIG_NOT_READABLE);
        }

        return null;
    }

    /**
     * Get the class of the concrete config object.
     * @return the config object class
     */
    protected abstract Class getConfigClass();

    /**
     * Reads the data from the buffer queue and sends it to Kafka.
     * Loops until the Hosebird client says it is done, which happens
     * when the client gets an error or it is stopped.
     * @param buffer buffers the Hosebird data
     * @param onwardQueueTopic the kafka topic to send data to
     * @param producer the Kafka producer
     * @param readAndSendPredicate whether to continue processing
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected void readAndSend(
            final LinkedBlockingQueue<String> buffer,
            final String onwardQueueTopic,
            final Producer<String, String> producer,
            final ReadAndSendPredicate readAndSendPredicate) {

        log.info("Start reading of messages from buffer queue and onward send");

        // If the client gets an error or is stopped then the predicate is true
        while (readAndSendPredicate.process()) {
            String message = null;
            try {
                this.log.trace("Reading message from Hosebird buffer");
                message = buffer.take();
                metrics.read.mark();
            } catch (InterruptedException e) {
                this.log.error(
                        "Error reading from buffer queue: {}",
                        e.getMessage());
                metrics.readError.mark();
            }

            // In order to log the message on failure we need to declare
            // it as final. If we decide not to log the message this
            // is unnecessary overhead.
            final String message2 = message;
            log.trace("Send message to Kafka: {}", message);
            log.trace("{} messages in buffer queue", buffer.size());
            producer.send(
                new ProducerRecord<>(onwardQueueTopic, "0", message),
                new Callback() {
                    @Override
                    public void onCompletion(
                            final RecordMetadata metadata,
                            final Exception exception) {
                        log.debug("Message sent to Kafka");
                        metrics.sent.mark();
                         if (exception != null) {
                            log.error(
                                "Exception sending message to Kafka: {}",
                                 exception.getMessage(),
                                    message2);
                             metrics.sendError.mark();
                         }
                    }
                }
            );
        }
    }

    /**
     * Logs the reason that the Hosebird client said it was done
     * by examining the exit event.
     * @param client the Hosebird client
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected void logClientExitReason(final Client client) {
        BasicClient bc = (BasicClient) client;
        Event e = bc.getExitEvent();

        log.error(
                "Hosebird client stopped: {} {}",
                new Object[]{
                e.getEventType().name(),
                e.getMessage()});
    }

    /**
     * Gets the Hosebird client.
     * @param buffer the queue which the client reads into
     * @param config the Hosebird configuration
     * @return the built Hosebird client
     */
    @SuppressWarnings("checkstyle:designforextension")
    protected abstract Client getHosebirdClient(
            final LinkedBlockingQueue<String> buffer,
            final Config config);

    /**
     * Create the Kafka producer.
     * @param config the Kafka configuration
     * @return the constructed producer
     */
    @SuppressWarnings("checkstyle:designforextension")
    protected Producer<String, String> getKafkaProducer(
            final KafkaConfig config) {

        log.info("Creating Kafka producer");

        Properties properties = new Properties();
        properties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                config.servers
                );

        properties.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");

        properties.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");

        // Deal with bad connection sensibly
        properties.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, "1000");
        properties.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, "1000");

        return new KafkaProducer<>(properties);
    }

    /**
     * Creates a ClientBuilder. Allows mocking in tests.
     * @return a new Hosebird ClientBuilder
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected ClientBuilder getClientBuilder() {
        return new ClientBuilder();
    }

    /**
     * Created the buffer queue. Allows mocking in tests.
     * @param size the size of the queue to create
     * @return the constructed queue
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected LinkedBlockingQueue<String> getBufferQueue(final int size) {
       return new LinkedBlockingQueue<>(size);
    }

    /**
     * Wrapper for adding shutdown hook. Allows testing.
     * @param thread the thread to run on shutdown
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected void runtimeAddShutdownHook(final Thread thread) {
        Runtime.getRuntime().addShutdownHook(thread);
    }

    /**
     * Wrapper for creating Metrics object. Allows testing.
     * @param stats The Hosebird client stats tracker
     * @return the created Metrics object
     */
    @SuppressWarnings("checkstyle:designforextension")
    protected Metrics getMetrics(final StatsReporter.StatsTracker stats) {
        return new Metrics(stats);
    }
}

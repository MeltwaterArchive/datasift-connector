package com.datasift.connector;

import com.datasift.connector.writer.Backoff;
import com.datasift.connector.writer.BulkManager;
import com.datasift.connector.writer.Messages;
import com.datasift.connector.writer.Metrics;
import com.datasift.connector.writer.SimpleConsumerManager;
import com.datasift.connector.writer.Sleeper;
import com.datasift.connector.writer.config.Config;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Entry class for the DataSiftWriter service.
 */
public class DataSiftWriter {

    /**
     * The logger to send messages to.
     */
    private static Logger log =
            LoggerFactory.getLogger(DataSiftWriter.class);

    /**
     * Service configuration object.
     */
    private Config config;

    /**
     * Whether the service should continue to execute.
     */
    private boolean processData = true;

    /**
     * Whether the service should continue to read from Kafka.
     */
    private boolean processRead = true;

    /**
     * Whether service should wait on Kafka connection to continue execution.
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected boolean waitOnKafka = true;

    /**
     * DataSift bulk manager.
     */
    @VisibleForTesting
    protected BulkManager bulkManager;

    /**
     * Kafka consumer group manager object.
     */
    @VisibleForTesting
    protected SimpleConsumerManager consumerManager;

    /**
     * Number of consumer threads.
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected int numConsumers = 1;

    /**
     * Main execution loop sleep time in milliseconds.
     */
    private static final int SLEEP_MAIN = 5000;

    /**
     * Milliseconds to wait on queue flushing before forcing service shutdown.
     */
    private static final int SLEEP_SHUTDOWN = 5000;

    /**
     * The Metrics container.
     */
    @VisibleForTesting
    protected Metrics metrics = new Metrics();

    /**
     * Get whether the writer should continue to process data.
     * @return boolean indicating whether to continue processing
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected boolean shouldProcess() {
        return processData;
    }

    /**
     * Sets whether the writer should continue to process data.
     * @param process indicates whether to continue processing
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected void shouldProcess(final boolean process) {
        processData = process;
    }

    /**
     * Get whether the service should continue to read from Kafka.
     * @return boolean indicating whether to continue reading
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected boolean shouldRead() {
        return processRead;
    }

    /**
     * Set the logger.
     * @param logger the logger to use
     */
    protected final void setLogger(final Logger logger) {
        this.log = logger;
    }

    /**
     * The constructor.
     */
    public DataSiftWriter() { }

    /**
     * The entry point for the service.
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        new DataSiftWriter().run(args);
    }

    /**
     * Pulls items from a Kafka queue and uploads them to an HTTP endpoint
     * until terminated.
     * @param args command line arguments
     */
    @SuppressWarnings("checkstyle:designforextension")
    protected final void run(final String[] args) {
        if (args.length == 0) {
            log.error(Messages.EXIT_ARGUMENTS_EMPTY);
            exit(1);
            return;
        }

        config = parseConfigFile(args[0]);
        if (config == null) {
            log.error(Messages.EXIT_CONFIG);
            exit(1);
            return;
        }

        log.info("Adding shutdown hook");
        addShutdownHook(SLEEP_SHUTDOWN);

        log.info("Creating the StatsD reporter");
        this.metrics.createAndStartStatsDReporter(
                metrics.getRegistry(),
                config.metrics.host,
                config.metrics.port,
                config.metrics.prefix,
                config.metrics.reportingTime);

        log.info("Initialising Kafka consumer manager");
        setupConsumer();

        log.info("Initialising bulk uploads");
        setupBulk(config, consumerManager, metrics, log);

        log.info("Entering service execution loop");
        while (shouldProcess()) {
            try {
                // Currently do nothing whilst consumer and streamer execute
                Thread.sleep(SLEEP_MAIN);
                logStatistics();
            } catch (InterruptedException e) {
                metrics.interruptedService.mark();
                log.debug("Service execution loop interrupted");
            }
        }
        log.info("Exiting service execution loop");
    }

    /**
     * Create and run Kafka consumer manager.
     */
    private void setupConsumer() {
        consumerManager = new SimpleConsumerManager(config, metrics);
        try {
            consumerManager.run(waitOnKafka);
        } catch (Exception e) {
            log.error("Error during start-up of Kafka consumer manager. Details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create and run bulk sending to the ingestion endpoint.
     * @param config the configuration
     * @param consumer the consumer client from which to pull data for this bulk sender
     * @param metrics the metrics
     * @param log the logger
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected void setupBulk(final Config config, final SimpleConsumerManager consumer,
                             final Metrics metrics, final Logger log) {
        bulkManager = new BulkManager(
                config.datasift,
                consumer,
                new Backoff(new Sleeper(), log, metrics),
                metrics);

        newSingleThreadExecutor().submit(bulkManager);
    }

    /**
     * Create a single thread executor.
     * Wrapper for testng.
     * @return the single thread executor
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected ExecutorService newSingleThreadExecutor() {
       return Executors.newSingleThreadExecutor();
    }

    /**
     * Add a hook that gracefully deals with shutdown.
     * Drain the buffer queue and send to Kafka before exit.
     * @param timeout maximum amount of milliseconds to wait for queue to drain
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected void addShutdownHook(final int timeout) {

        this.runtimeAddShutdownHook(new Thread() {
            public void run() {
                log.info("Shutdown process initiated");
                metrics.shutdown.mark();

                // Let the service loop know we're stopping & flag to consumer to stop reading
                shouldProcess(false);

                // Stop the bulk manager
                bulkManager.shutdown();

                // Stop the consumer manager
                consumerManager.shutdown();

                logStatistics();

                log.info("Shutdown process completed");
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
     * Wrapper for adding shutdown hook. Allows testing.
     * @param thread the thread to run on shutdown
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected void runtimeAddShutdownHook(final Thread thread) {
        Runtime.getRuntime().addShutdownHook(thread);
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
            Config config =  mapper.readValue(new File(jsonFile), Config.class);
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
     * Outputs statistics on writer execution to the current logger.
     */
    @SuppressWarnings("checkstyle:designforextension")
    protected void logStatistics() {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        String msg = "STATUS - ";
        String uptimeStr = String.format(
                "%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(uptime),
                TimeUnit.MILLISECONDS.toSeconds(uptime)
                        - TimeUnit.MINUTES.toSeconds(
                            TimeUnit.MILLISECONDS.toMinutes(uptime)
                        )
        );
        msg += "JVM uptime: " + uptimeStr + ". ";
        msg += "Total Kafka items read: "
                + metrics.readKafkaItem.getCount() + ". ";
        msg += "Total items sent to endpoint: "
                + metrics.sentItems.getCount() + ".";

        log.info(msg);
    }
}

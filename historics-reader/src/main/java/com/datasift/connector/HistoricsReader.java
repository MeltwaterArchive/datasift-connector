package com.datasift.connector;

import com.datasift.connector.reader.Metrics;
import com.datasift.connector.reader.config.Config;
import com.datasift.connector.reader.config.KafkaConfig;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import com.google.common.annotations.VisibleForTesting;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 *
 */
public class HistoricsReader {

    /**
     * The metrics.
     */
    @VisibleForTesting
    protected Metrics metrics = null;

    /**
     *
     */
    private Connection dbConn = null;

    /**
     *
     */
    private Producer<String, String> producer = null;

    /**
     * The logger to send messages to.
     */
    private static Logger log =
            LoggerFactory.getLogger(HistoricsReader.class);

    /**
     * Configuration object.
     */
    private Config config;

    /**
     *
     */
    private static final long UNIX_TIME_DIVISOR = 1000L;

    /**
     * The constructor.
     */
    public HistoricsReader() { }

    /**
     * The entry point for the service.
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        new HistoricsReader().run(args);
    }

    /**
     *
     * @param args command line arguments
     */
    public final void run(final String[] args) {
        long unixTime = System.currentTimeMillis() / UNIX_TIME_DIVISOR;
        log.info("Historics reader processing initiated at " + unixTime);

        // Parse the config file provided
        config = parseConfigFile(args[0]);
        if (config == null) {
            log.error("Could not parse configuration file");
            System.exit(1);
            return;
        }

        // Client to send messages to the onward queue
        producer = getKafkaProducer(config.kafka);

        log.info("Job processing starting...");
        updateJobs();

        String id = lockJob();
        while (id != null) {
            setProcessing(id);
            updateGNIPURLs(id);
            processURLs(id);
            setDone(id);
            unlockJob(id);

            id = lockJob();
        }

        log.info("No more delivered jobs could be locked for processing.");
        producer.close();
    }

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
     *
     * @param path the API path to append to the URI
     * @return URI object representing the full path resource
     */
    protected final URI getUri(final String path) {
        String baseURL = config.gnip.baseURL;
        if (!baseURL.endsWith("/")) {
            baseURL += "/";
        }

        String url = baseURL + path;
        URI uri = null;
        try {
            uri = new URIBuilder(url).setPort(config.gnip.port).build();
        } catch (URISyntaxException e) {
            String msg = "GNIP configuration base URL "
                    + "and/or port syntax invalid";
            log.error(msg);
            return null;
        }

        return uri;
    }

    /**
     * Returns a Historics Reader configuration object parsed from JSON file.
     * @param jsonFile String file path of the JSON file to parse
     * @return a Config object representing JSON data provided
     */
    @VisibleForTesting
    @SuppressWarnings("checkstyle:designforextension")
    protected Config parseConfigFile(final String jsonFile) {

        log.info("Parsing configuration file");

        try {
            ObjectMapper mapper = new ObjectMapper();
            Config config =  (Config) mapper.readValue(new File(jsonFile), Config.class);
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

            log.error("{} {}", "Could not read all required configuration properties", constraintErrors);
            return null;

        } catch (JsonMappingException | JsonParseException e) {
            log.error("Configuration file is not valid JSON");
        } catch (IOException e) {
            log.error("Configuration file is not readable");
        }

        return null;
    }

    /**
     *
     * @return id of the locked job. if no job was locked, returns null
     */
    private String lockJob() {
        try {
            String connStr = "jdbc:sqlite:" + config.database.filepath;
            log.info("Connecting to sqlite DB using string: " + connStr);
            dbConn = DriverManager.getConnection(connStr);

            String lockStr = ManagementFactory.getRuntimeMXBean().getName();

            Statement checkStmt = dbConn.createStatement();
            ResultSet rs = checkStmt.executeQuery(
                    "SELECT * FROM jobs WHERE lock='' AND status='delivered' ORDER BY ADDED_AT ASC LIMIT 1");

            String idToLock = null;
            if (rs.next()) {
                idToLock = rs.getString("id");
                Statement lockStmt = dbConn.createStatement();
                lockStmt.executeUpdate("UPDATE jobs SET lock='" + lockStr + "' WHERE id='" + idToLock + "'");
                log.info("Locked job " + idToLock + " in DB");
                return idToLock;
            } else {
                return null;
            }
        } catch (SQLException e) {
            log.error("Database error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            log.error("Error whilst locking job in DB: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (dbConn != null) {
                    dbConn.close();
                }
            } catch (SQLException e) {
                log.error("Error whilst closing database connection: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     *
     */
    private void updateJobs() {
        try {
            String connStr = "jdbc:sqlite:" + config.database.filepath;
            dbConn = DriverManager.getConnection(connStr);

            Statement getIdleJobsStmt = dbConn.createStatement();
            ResultSet rs = getIdleJobsStmt.executeQuery(
                    "SELECT * FROM jobs WHERE status!='done' AND status!='processing'");

            while (rs.next()) {
                String jobId = rs.getString("id");
                String status = fetchJobStatusFromGNIP(jobId);
                Statement updateJobStmt = dbConn.createStatement();
                updateJobStmt.executeUpdate("UPDATE jobs SET status='" + status + "' WHERE id='" + jobId + "'");
                log.info("Updated status for job " + jobId + " to " + status);
            }
        } catch (SQLException e) {
            log.error("Database error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            log.error("Error whilst updating job DB statuses: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (dbConn != null) {
                    dbConn.close();
                }
            } catch (SQLException e) {
                log.error("Error whilst closing database connection: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     *
     * @param jobId job for which to look up the status
     * @return status of given job returned from GNIP API
     */
    private String fetchJobStatusFromGNIP(final String jobId) {
        try {
            String json = Request.Get(
                    getUri("accounts/" + config.gnip.accountName
                                    + "/publishers/twitter/historical/track/jobs/"
                                    + jobId
                                    + ".json"
                    )
            ).execute().returnContent().asString();

            JSONObject obj = new JSONObject(json);
            String status = obj.getString("status");
            log.info("Read status from GNIP: " + status + " for job " + jobId);
            return status;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     *
     * @param jobId job for which to set status
     */
    private void setProcessing(final String jobId) {
        try {
            String connStr = "jdbc:sqlite:" + config.database.filepath;
            log.info("Connecting to sqlite DB using string: " + connStr);
            dbConn = DriverManager.getConnection(connStr);

            Statement setStmt = dbConn.createStatement();
            setStmt.executeUpdate("UPDATE jobs SET status='processing' WHERE id='" + jobId + "'");
            log.info("Job " + jobId + " set to processing in DB");
        } catch (SQLException e) {
            log.error("Error whilst updating job status to processing for id " + jobId + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (dbConn != null) {
                    dbConn.close();
                }
            } catch (SQLException e) {
                log.error("Error whilst closing database connection: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     *
     * @param jobId job for which to update result URLs
     */
    private void updateGNIPURLs(final String jobId) {
        try {
            String json = Request.Get(
                    getUri("accounts/" + config.gnip.accountName
                            + "/publishers/twitter/historical/track/jobs/"
                            + jobId
                            + "/results.json"
                    )
            ).execute().returnContent().asString();
            JSONObject obj = new JSONObject(json);
            String suspectMinutesURL = obj.getString("suspectMinutesUrl");
            String urls = obj.getJSONArray("urlList").toString();

            String connStr = "jdbc:sqlite:" + config.database.filepath;
            log.info("Connecting to sqlite DB using string: " + connStr);
            dbConn = DriverManager.getConnection(connStr);

            if (suspectMinutesURL != null) {
                Statement setSuspectMinutesStmt = dbConn.createStatement();
                setSuspectMinutesStmt.executeUpdate(
                        "UPDATE jobs SET suspect_minutes_url='" + suspectMinutesURL + "' WHERE id='" + jobId + "'");
            }

            Statement setUrlsStmt = dbConn.createStatement();
            setUrlsStmt.executeUpdate(
                    "UPDATE jobs SET urls='" + urls + "' WHERE id='" + jobId + "'");
        } catch (Exception e) {
            log.error("Error updating result URLs from GNIP for job " + jobId + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (dbConn != null) {
                    dbConn.close();
                }
            } catch (SQLException e) {
                log.error("Error whilst closing database connection: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     *
     * @param jobId URLs to be processed will be read from this job
     */
    private void processURLs(final String jobId) {
        try {
            String connStr = "jdbc:sqlite:" + config.database.filepath;
            log.info("Connecting to sqlite DB using string: " + connStr);
            dbConn = DriverManager.getConnection(connStr);

            Statement getUrlsStmt = dbConn.createStatement();
            ResultSet rs = getUrlsStmt.executeQuery(
                    "SELECT urls FROM jobs WHERE id='" + jobId + "'");
            JSONArray arr = new JSONArray(rs.getString("urls"));
            for (int i = 0; i < arr.length(); i++) {
                String url = arr.getString(i);
                processURL(url);
            }
        } catch (Exception e) {
            log.error("Error whilst looping through result URLs for job " + jobId + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (dbConn != null) {
                    dbConn.close();
                }
            } catch (SQLException e) {
                log.error("Error whilst closing database connection: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     *
     * @param url resource to download and process
     */
    private void processURL(final String url) {
        log.info("Processing URL: " + url);
        String filePath = downloadFile(url);

        try {
            InputStream fileStream = new FileInputStream(filePath);
            InputStream gzipStream = new GZIPInputStream(fileStream);
            Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
            BufferedReader buffered = new BufferedReader(decoder);

            log.info("Reading lines from file " + filePath);
            String line = buffered.readLine().trim();
            while (line != null) {
                try {
                    //JSONObject obj = new JSONObject(line);
                    //if (obj.get("info") == null) {
                        final String line2 = line;
                        log.debug("Sending interaction. Hash: " + line.hashCode());
                        producer.send(
                                new ProducerRecord<>(config.kafka.topic, "0", line),
                                new Callback() {
                                    @Override
                                    public void onCompletion(
                                            final RecordMetadata metadata,
                                            final Exception exception) {
                                        log.debug("Message sent to Kafka");
                                        if (exception != null) {
                                            log.error(
                                                    "Exception sending message to Kafka: {}",
                                                    exception.getMessage(),
                                                    line2);
                                        }
                                    }
                                }
                        );
                    //}
                } catch (JSONException e) {
                    log.debug("Discarded line as it is not valid JSON: " + line);
                } finally {
                    line = buffered.readLine();
                }
            }

            log.info("Deleting processed file " + filePath);
            File tmpFile = new File(filePath);
            Files.delete(tmpFile.toPath());

        } catch (Exception e) {
            log.error("Error whilst processing gzipped interaction file " + filePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     *
     * @param url resource to download
     * @return full file path of the downloaded file
     */
    private String downloadFile(final String url) {
        try {
            URL website = new URL(url);
            String path = "/tmp" + website.getFile();
            log.info("Downloading file to " + path);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(path);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            log.info("File downloaded to: " + path);
            return path;
        } catch (Exception e) {
            log.error("Error whilst downloading file " + url + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     *
     * @param jobId job for which to set status
     */
    private void setDone(final String jobId) {
        try {
            String connStr = "jdbc:sqlite:" + config.database.filepath;
            log.info("Connecting to sqlite DB using string: " + connStr);
            dbConn = DriverManager.getConnection(connStr);

            Statement setStmt = dbConn.createStatement();
            setStmt.executeUpdate("UPDATE jobs SET status='done' WHERE id='" + jobId + "'");
            log.info("Job " + jobId + "set to done in DB");
        } catch (SQLException e) {
            log.error("Error whilst updating status to done for job " + jobId + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (dbConn != null) {
                    dbConn.close();
                }
            } catch (SQLException e) {
                log.error("Error whilst closing database connection: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     *
     * @param jobId job to unlock in the database
     */
    private void unlockJob(final String jobId) {
        try {
            String connStr = "jdbc:sqlite:" + config.database.filepath;
            log.info("Connecting to sqlite DB using string: " + connStr);
            dbConn = DriverManager.getConnection(connStr);

            Statement setStmt = dbConn.createStatement();
            setStmt.executeUpdate("UPDATE jobs SET lock='' WHERE id='" + jobId + "'");
            log.info("Job " + jobId + "unlocked in DB");
        } catch (SQLException e) {
            log.error("Error whilst unlocking job " + jobId + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (dbConn != null) {
                    dbConn.close();
                }
            } catch (SQLException e) {
                log.error("Error whilst closing database connection: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}

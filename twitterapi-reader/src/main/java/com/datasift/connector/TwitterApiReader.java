package com.datasift.connector;

import com.datasift.connector.reader.config.Config;
import com.datasift.connector.reader.config.TwitterApiConfig;
import com.datasift.connector.reader.config.TwitterApiReaderConfig;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.LineStringProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Reads data from twitter streaming api.
 */
public class TwitterApiReader extends HosebirdReader {

    /**
     * Creates the logger.
     * @return the created logger
     */
    @Override
    protected final Logger createLogger() {
        return LoggerFactory.getLogger(TwitterApiReader.class);
    }

    /**
     * The constructor.
     */
    public TwitterApiReader() {
        super();
    }

    /**
     * Gets the class for the config.
     * @return the config class
     */
    @SuppressWarnings("checkstyle:designforextension")
    @Override
    protected Class getConfigClass() {
        return TwitterApiReaderConfig.class;
    }

    /**
     * The entry point for the service.
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        new TwitterApiReader().run(args);
    }


    /**
     * Gets the Twitter client.
     * @param buffer the queue which the client reads into
     * @param readerConfig the Twitter reader configuration
     * @return the built Twitter client
     */
    @SuppressWarnings("checkstyle:designforextension")
    @Override
    protected Client getHosebirdClient(
            final LinkedBlockingQueue<String> buffer,
            final Config readerConfig) {

        getLogger().info("Building Twitter client");

        StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();

        TwitterApiConfig config = ((TwitterApiReaderConfig) readerConfig).twitterapi;

        // Term tracking
        if (config.keywords != null) {
            endpoint.trackTerms(config.keywords);
        }

        // User ID tracking
        if (config.userIds != null) {
            endpoint.followings(config.userIds);
        }

        // Authentication
        Authentication auth = new OAuth1(
                config.consumerKey,
                config.consumerSecret,
                config.accessToken,
                config.accessSecret
        );

        // Processor
        LineStringProcessor processor =
                new LineStringProcessor(buffer, readerConfig.hosebird.bufferTimeout);

        // Create a new BasicClient. By default gzip is enabled.
        return this.getClientBuilder()
                .name("Twitter Api Reader")
                .hosts(Constants.STREAM_HOST)
                .endpoint(endpoint)
                .authentication(auth)
                .retries(readerConfig.hosebird.retries)
                .processor(processor)
                .build();
    }
}

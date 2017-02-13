package com.datasift.connector;

import com.datasift.connector.reader.config.Config;
import com.datasift.connector.reader.config.GnipReaderConfig;
import com.datasift.connector.reader.config.GnipConfig;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.endpoint.RealTimeEnterpriseStreamingEndpoint_v2;
import com.twitter.hbc.core.processor.LineStringProcessor;
import com.twitter.hbc.httpclient.auth.BasicAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Reads data from Gnip.
 */
public class GnipReader extends HosebirdReader {

    /**
     * Creates the logger.
     * @return the created logger
     */
    @Override
    protected final Logger createLogger() {
        return LoggerFactory.getLogger(GnipReader.class);
    }

    /**
     * The constructor.
     */
    public GnipReader() {
        super();
    }

    /**
     * Gets the class for the config.
     * @return the config class
     */
    @SuppressWarnings("checkstyle:designforextension")
    @Override
    protected Class getConfigClass() {
        return GnipReaderConfig.class;
    }

    /**
     * The entry point for the service.
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        new GnipReader().run(args);
    }


    /**
     * Gets the Gnip client.
     * @param buffer the queue which the client reads into
     * @param readerConfig the Gnip Reader configuration
     * @return the built Gnip client
     */
    @SuppressWarnings("checkstyle:designforextension")
    @Override
    protected Client getHosebirdClient(
              final LinkedBlockingQueue<String> buffer,
              final Config readerConfig) {

        getLogger().info("Building Gnip client");

        GnipConfig config = ((GnipReaderConfig) readerConfig).gnip;

        RealTimeEnterpriseStreamingEndpoint_v2 endpoint =
                new RealTimeEnterpriseStreamingEndpoint_v2(
                        config.account,
                        config.product,
                        config.label);
        BasicAuth auth = new BasicAuth(config.username, config.password);
        LineStringProcessor processor =
                new LineStringProcessor(buffer, readerConfig.hosebird.bufferTimeout);

        return this.getClientBuilder()
                .name("Gnip Reader")
                .hosts(config.host)
                .endpoint(endpoint)
                .authentication(auth)
                .processor(processor)
                .retries(readerConfig.hosebird.retries)
                .build();
    }
}

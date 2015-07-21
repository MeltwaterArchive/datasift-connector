package com.datasift.connector;

import com.datasift.connector.reader.config.HosebirdConfig;
import com.datasift.connector.reader.config.GnipConfig;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.endpoint.RealTimeEnterpriseStreamingEndpoint;
import com.twitter.hbc.core.processor.LineStringProcessor;
import com.twitter.hbc.httpclient.auth.BasicAuth;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Reads data from Gnip.
 */
public class GnipReader extends HosebirdReader {

    /**
     * The constructor.
     */
    public GnipReader() {
        super();
        this.setLogger(LoggerFactory.getLogger(GnipReader.class));
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
     * @param hbcConfig the Gnip configuration
     * @return the built Gnip client
     */
    @SuppressWarnings("checkstyle:designforextension")
    protected Client getHosebirdClient(
            final LinkedBlockingQueue<String> buffer,
            final HosebirdConfig hbcConfig) {

        log.info("Building Gnip client");

        GnipConfig config = (GnipConfig) hbcConfig;

        RealTimeEnterpriseStreamingEndpoint endpoint =
                new RealTimeEnterpriseStreamingEndpoint(
                        config.account,
                        config.product,
                        config.label);
        BasicAuth auth = new BasicAuth(config.username, config.password);
        LineStringProcessor processor =
                new LineStringProcessor(buffer, config.bufferTimeout);

        return this.getClientBuilder()
                .name("Gnip Reader")
                .hosts(config.host)
                .endpoint(endpoint)
                .authentication(auth)
                .processor(processor)
                .retries(config.retries)
                .build();
    }
}

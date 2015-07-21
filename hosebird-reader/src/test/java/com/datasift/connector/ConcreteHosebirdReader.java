package com.datasift.connector;

import com.datasift.connector.reader.config.Config;
import com.twitter.hbc.core.Client;
import org.slf4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;

import static org.mockito.Mockito.*;

public class ConcreteHosebirdReader extends HosebirdReader {
    @Override
    protected Logger createLogger() {
        return mock(Logger.class);
    }

    @Override
    protected Class<ConcreteHosebirdConfig> getConfigClass() {
        return ConcreteHosebirdConfig.class;
    }

    @Override
    protected Client getHosebirdClient(LinkedBlockingQueue<String> buffer, Config config) {
        return this.getClientBuilder().build();
    }
}

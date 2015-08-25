package com.datasift.connector.writer;

import com.datasift.connector.writer.config.Config;
import com.datasift.connector.writer.config.ZookeeperConfig;
import kafka.api.FetchRequest;
import kafka.api.FetchRequestBuilder;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.common.ErrorMapping;
import kafka.common.OffsetAndMetadata;
import kafka.common.TopicAndPartition;

import kafka.javaapi.FetchResponse;
import kafka.javaapi.OffsetCommitRequest;
import kafka.javaapi.OffsetCommitResponse;
import kafka.javaapi.OffsetResponse;
import kafka.javaapi.PartitionMetadata;
import kafka.javaapi.TopicMetadata;
import kafka.javaapi.TopicMetadataRequest;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.message.MessageAndOffset;
import kafka.utils.ZKStringSerializer$;
import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Low level consumer exposing the ConsumerManager functionality.
 */
public class SimpleConsumerManager implements ConsumerManager {

    /**
     * The logger to send messages to.
     */
    private static Logger log =
            LoggerFactory.getLogger(SimpleConsumerManager.class);

    /**
     * Configuration object containing Kafka & Zookeeper specific properties.
     */
    private Config config;

    /**
     * Metrics object for reporting.
     */
    private Metrics metrics;

    /**
     * Kafka topic for which to read items.
     */
    private String topic;

    /**
     * Partition ID to read from under broker.
     */
    private int partition;

    /**
     * List of broker IPs to gather offsets & metadata from.
     */
    private List<String> seedBrokers;

    /**
     * Port on which Kafka broker is running.
     */
    private int port;

    /**
     * Low level Kafka consumer client used for all requests.
     */
    private SimpleConsumer consumer;

    /**
     * IP of the leader for current topic.
     */
    private String leadBroker;

    /**
     * Client ID included in requests based off of topic, partition.
     */
    private String clientName;

    /**
     * Offset from which to perform next queue read.
     */
    private long readOffset;

    /**
     * Offset of the message which was last processed and sent onward.
     */
    private long lastReturnedOffset;

    /**
     * List containing brokers which replicate data.
     */
    private List<String> replicaBrokers = new ArrayList<String>();

    /**
     * Cache to store items read from Kafka fetch requests, prior to processing.
     */
    private Queue<ConsumerData> dataItems = new ArrayDeque<ConsumerData>();

    /**
     * Zookeeper client. Used for retrieval of broker information.
     */
    private ZkClient zkClient;

    /**
     * Zookeeper configuration properties.
     */
    private ZookeeperConfig zkConfig;

    /**
     * Consumer group ID used in offset requests.
     */
    private static final String GROUP_ID = "writer";

    /**
     * Attempts at fetching data from Kafka before stating error.
     */
    private static final int KAFKA_MAX_READ_ERRORS = 5;

    /**
     * Attempts at retrieving data for a new lead broker before stating error.
     */
    private static final int NEW_LEADER_TRIES = 3;

    /**
     * Milliseconds to sleep whilst waiting for new lead broker to sync with Zookeeper.
     */
    private static final int NEW_LEADER_PAUSE_MS = 1000;

    /**
     * Amount of bytes to read from Kafka in each fetch request.
     */
    private static final int BYTES_TO_READ = 100000;

    /**
     * Milliseconds to wait until timeout when connecting to a broker.
     */
    private static final int CONSUMER_TIMEOUT = 100000;

    /**
     * Size of buffer, in bytes, used by consumer client.
     */
    private static final int CONSUMER_BUFFER_SIZE = 64 * 1024;

    /**
     * Milliseconds to wait until timeout when connected to ZooKeeper.
     */
    private static final int ZOOKEEPER_SESSION_TIMEOUT = 100000;

    /**
     * Milliseconds to wait until timeout when connecting to ZooKeeper.
     */
    private static final int ZOOKEEPER_CONNECTION_TIMEOUT = 100000;

    /**
     * Main constructor.
     * @param config configuration object containing Kafka & ZK specific properties
     * @param metrics Shared metrics object to report to
     */
    public SimpleConsumerManager(final Config config, final Metrics metrics) {
        this.config = config;
        this.metrics = metrics;
        topic = config.kafka.topic;
        partition = 0;
        seedBrokers = new ArrayList<String>();
        seedBrokers.add(config.kafka.broker);
        port = config.kafka.port;
        replicaBrokers = new ArrayList<String>();
        zkConfig = config.zookeeper;
    }

    /**
     * Get the next item from the Kafka queue containing the offset and current message.
     * @return consumed data item as ConsumerData object or null if no item to consume
     */
    @SuppressWarnings("checkstyle:designforextension")
    public ConsumerData readItem() {

        if (!dataItems.isEmpty()) {
            // there are cached items from previous read to return
            ConsumerData item = dataItems.remove();
            lastReturnedOffset = item.getOffset();
            log.debug("Consumer returning cached Kafka message. Offset: "
                    + lastReturnedOffset
                    + " Hash: " + item.hashCode());
            metrics.passedOnKafkaItem.mark();
            return item;
        }

        // read more data from broker
        FetchResponse fetchResponse = null;
        int numErrors = 0;

        while (numErrors < KAFKA_MAX_READ_ERRORS) {
            if (consumer == null) {
                consumer = new SimpleConsumer(leadBroker, port, CONSUMER_TIMEOUT, CONSUMER_BUFFER_SIZE, clientName);
            }
            FetchRequest req = new FetchRequestBuilder()
                    .clientId(clientName)
                    .addFetch(topic, partition, readOffset, BYTES_TO_READ)
                    .build();
            log.trace("Reading " + BYTES_TO_READ
                    + " from Kafka broker " + leadBroker + ":" + port
                    + " with offset " + readOffset);
            fetchResponse = consumer.fetch(req);

            if (fetchResponse.hasError()) {
                numErrors++;

                short code = fetchResponse.errorCode(topic, partition);
                log.error("Kafka error fetching data from the Broker:" + leadBroker + " Reason code: " + code);
                if (numErrors == KAFKA_MAX_READ_ERRORS) {
                    log.error("Kafka consumer reached maximum number of read errors, aborting read");
                    return null;
                }
                if (code == ErrorMapping.OffsetOutOfRangeCode()) {
                    // We asked for an invalid offset. For simple case ask for the last element to reset
                    readOffset = getLastOffset(
                            consumer, topic, partition, kafka.api.OffsetRequest.LatestTime(), clientName
                    );
                    log.error("Consumer requested an invalid offset. Resetting to " + readOffset);
                    continue;
                }
                consumer.close();
                consumer = null;
                try {
                    log.debug("Consumer encountered an error with latest fetch. Attempting to find new leader");
                    leadBroker = findNewLeader(leadBroker, topic, partition, port);
                } catch (Exception e) {
                    log.error("Kafka consumer aborting read due to exception thrown finding leader");
                    return null;
                }
                continue;
            } else {
                // Fetch appeared to be successful
                log.trace("Consumer fetch has a response. Moving on to processing");
                break;
            }
        }

        if (fetchResponse == null) {
            log.error("Consumer could not retrieve Kafka fetch response");
            return null;
        }

        // process and cache messages read
        long numRead = 0;
        for (MessageAndOffset messageAndOffset : fetchResponse.messageSet(topic, partition)) {
            long currentOffset = messageAndOffset.offset();
            log.debug("Consumer processing received message with offset " + currentOffset);
            if (currentOffset < readOffset) {
                log.error("Found an old offset: " + currentOffset + " Expecting: " + readOffset);
                continue;
            }

            try {
                // read message bytes
                ByteBuffer payload = messageAndOffset.message().payload();
                byte[] bytes = new byte[payload.limit()];
                payload.get(bytes);

                // convert message
                String data = new String(bytes, "UTF-8");
                ConsumerData item = new ConsumerData(currentOffset, data);
                log.debug("Adding new data item to consumer cache. Hash: " + item.hashCode());
                dataItems.add(item);
                metrics.readKafkaItem.mark();
            } catch (UnsupportedEncodingException e) {
                log.error("Consumer error converting kafka item to String: " + e.getMessage());
                return null;
            }

            numRead++;
        }

        if (numRead > 0) {
            log.debug("Consumer has read " + numRead + " messages into local cache");

            ConsumerData item = dataItems.remove();
            lastReturnedOffset = item.getOffset();
            log.debug("Consumer sending message onwards. Hash: " + item.hashCode());
            metrics.passedOnKafkaItem.mark();
            return item;
        } else {
            log.debug("No messages found to read from Kafka broker " + leadBroker + " at offset " + readOffset);
            return null;
        }
    }

    /**
     * Commit the last read item offset to Kafka. The next item read after a successful commit will
     * succeed the last read in the Kafka queue. Items read before the commit will no longer be
     * readable.
     * @return boolean indicating whether the commit was successful
     */
    @SuppressWarnings("checkstyle:designforextension")
    public boolean commit() {
        if (consumer == null) {
            consumer = new SimpleConsumer(leadBroker, port, CONSUMER_TIMEOUT, CONSUMER_BUFFER_SIZE, clientName);
        }
        if (lastReturnedOffset == -1) {
            log.error("Kafka offset commit cannot be completed as no items have been returned from the consumer");
            return false;
        } else {
            HashMap<TopicAndPartition, OffsetAndMetadata> map = new HashMap<>();
            map.put(
                    new TopicAndPartition(topic, 0),
                    new OffsetAndMetadata(lastReturnedOffset, "", -1L)
            );
            OffsetCommitRequest request = new OffsetCommitRequest(GROUP_ID, map, 0, clientName);

            OffsetCommitResponse response = consumer.commitOffsets(request);
            if (response.hasError()) {
                log.error("Error encountered whilst committing offset " + lastReturnedOffset + " for Kafka");
                log.error("Commit error code: " + response.errorCode(new TopicAndPartition(topic, 0)));
                return false;
            } else {
                // update offset from which to read on next request
                readOffset = lastReturnedOffset;
                log.debug("Consumer has committed offset " + lastReturnedOffset);
                return true;
            }
        }
    }

    /**
     * Reset the read position back to the last commit. The next item read after a reset will
     * return the message following the committed offset. Items read since the last commit will
     * be returned again.
     */
    @SuppressWarnings("checkstyle:designforextension")
    public boolean reset() {
        if (dataItems != null) {
            dataItems.clear();

            if (dataItems.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gracefully shut down the consumer.
     */
    public final void shutdown() {
        log.info("Shutting down Kafka consumer manager");
        if (consumer != null) {
            consumer.close();
        }
        if (zkClient != null) {
            zkClient.close();
            zkClient = null;
        }
    }

    /**
     * Gathers leader & partition information for topic.
     * Sets up client ID.
     * Assigns initial read offset for consumer.
     */
    public final void run() {
        connectZk();

        // find the meta data about the topic and partition we are interested in
        PartitionMetadata metadata = findLeader(seedBrokers, port, topic, partition);
        if (metadata == null) {
            log.error("Kafka consumer can't find metadata for topic: " + topic + " & partition: " + partition);
            return;
        }
        if (metadata.leader() == null) {
            log.error("Kafka consumer can't find leader for topic: " + topic + " & partition: " + partition);
            return;
        }
        leadBroker = metadata.leader().host();
        clientName = "Client_" + topic + "_" + partition;

        log.info("Consumer is connecting to lead broker " + leadBroker + ":" + port + " under client id " + clientName);
        consumer = new SimpleConsumer(leadBroker, port, CONSUMER_TIMEOUT, CONSUMER_BUFFER_SIZE, clientName);
        readOffset = getLastOffset(consumer, topic, partition, kafka.api.OffsetRequest.EarliestTime(), clientName);
        log.info("Consumer is going to being reading from offset " + readOffset);
    }

    /**
     * Initialises a connection to Zookeeper via zkClient.
     */
    private void connectZk() {
        log.info("Consumer connecting to zookeeper instance at " + zkConfig.socket);
        zkClient = new ZkClient(
                zkConfig.socket,
                ZOOKEEPER_SESSION_TIMEOUT,
                ZOOKEEPER_CONNECTION_TIMEOUT,
                ZKStringSerializer$.MODULE$
        );
    }

    /**
     * Retrieves latest committed offset for a given topic & partition. Uses the
     * new Kafka offset storage API introduced in 0.8.1.
     * @param consumer consumer client to use for request
     * @param topic topic id for which to lookup offset
     * @param partition partition id for which to lookup offset
     * @param whichTime OffsetRequest constant defining which offset is preferred
     * @param clientName client id to include in request
     * @return the offset returned from the lead broker
     */
    private static long getLastOffset(final SimpleConsumer consumer, final String topic, final int partition,
                                      final long whichTime, final String clientName) {
        TopicAndPartition topicAndPartition = new TopicAndPartition(topic, partition);
        Map<TopicAndPartition, PartitionOffsetRequestInfo> requestInfo = new HashMap<>();
        requestInfo.put(topicAndPartition, new PartitionOffsetRequestInfo(whichTime, 1));
        kafka.javaapi.OffsetRequest request = new kafka.javaapi.OffsetRequest(
                requestInfo, kafka.api.OffsetRequest.CurrentVersion(), clientName);
        OffsetResponse response = consumer.getOffsetsBefore(request);

        if (response.hasError()) {
            log.error("Error fetching offset data from the Broker. Reason: " + response.errorCode(topic, partition));
            return 0;
        }
        long[] offsets = response.offsets(topic, partition);
        return offsets[0];
    }

    /**
     * Updates and assigns new lead broker for given topic & partition based on metadata from
     * old broker. Retries automatically in scenario whereby Zookeeper has not immediately
     * rebalanced.
     * @param oldLeader broker which was previously the leader
     * @param topic topic for which to acquire new leader
     * @param partition partition for which to acquire new leader
     * @param port port on which kafka broker runs
     * @return hostname of new lead broker
     * @throws Exception when no leader has been identified
     */
    private String findNewLeader(final String oldLeader, final String topic, final int partition, final int port)
            throws Exception {
        for (int i = 0; i < NEW_LEADER_TRIES; i++) {
            boolean goToSleep = false;
            PartitionMetadata metadata = findLeader(replicaBrokers, port, topic, partition);
            if (metadata == null) {
                goToSleep = true;
            } else if (metadata.leader() == null) {
                goToSleep = true;
            } else if (oldLeader.equalsIgnoreCase(metadata.leader().host()) && i == 0) {
                // first time through if the leader hasn't changed give ZooKeeper a second to recover
                // second time, assume the broker did recover before failover, or it was a non-Broker issue
                goToSleep = true;
            } else {
                return metadata.leader().host();
            }
            if (goToSleep) {
                try {
                    Thread.sleep(NEW_LEADER_PAUSE_MS);
                } catch (InterruptedException ie) {
                }
            }
        }
        log.error("Kafka consumer Unable to find new leader after broker failure");
        throw new Exception("consumer-no-leader-found");
    }

    /**
     * Makes requests for topic metadata to each seed broker. Populates list of replica brokers.
     * Returns the lead broker for given topic & partition.
     * @param seedBrokers list of broker hosts to query for metadata
     * @param port port on which kafka broker runs
     * @param topic topic for which to acquire a lead broker
     * @param partition partition for which to acquire a lead broker
     * @return metadata for lead broker if one is found, otherwise null
     */
    private PartitionMetadata findLeader(final List<String> seedBrokers, final int port,
                                               final String topic, final int partition) {
        PartitionMetadata returnMetaData = null;
        loop:
        for (String seed : seedBrokers) {
            SimpleConsumer consumer = null;
            try {
                log.info("Consumer looking up leader for " + topic + ", " + partition + " at " + seed + ":" + port);
                consumer = new SimpleConsumer(seed, port, CONSUMER_TIMEOUT, CONSUMER_BUFFER_SIZE, "leaderLookup");
                List<String> topics = Collections.singletonList(topic);
                TopicMetadataRequest req = new TopicMetadataRequest(topics);
                kafka.javaapi.TopicMetadataResponse resp = consumer.send(req);

                List<TopicMetadata> metaData = resp.topicsMetadata();
                for (TopicMetadata item : metaData) {
                    for (PartitionMetadata part : item.partitionsMetadata()) {
                        if (part.partitionId() == partition) {
                            returnMetaData = part;
                            break loop;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error communicating with Broker [" + seed + "] to find Leader for [" + topic
                        + ", " + partition + "] Reason: " + e);
            } finally {
                if (consumer != null) {
                    consumer.close();
                }
            }
        }
        if (returnMetaData != null) {
            replicaBrokers.clear();
            for (kafka.cluster.Broker replica : returnMetaData.replicas()) {
                replicaBrokers.add(replica.host());
            }
        }
        return returnMetaData;
    }
}

package com.matrut.mykafka.consumer;

import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseConsumer {

    protected static final Logger logger = LogManager.getLogger();
    protected final KafkaConsumer<String, String> consumer;
    protected final List<String> topics;
    protected final AtomicBoolean shutdown;
    protected final CountDownLatch shutdownLatch;

    public BaseConsumer(Properties config, List<String> topics) {
        this.consumer = new KafkaConsumer<>(config);
        this.topics = topics;
        this.shutdown = new AtomicBoolean(false);
        this.shutdownLatch = new CountDownLatch(1);
    }

    protected void doCommitSync() {
        try {
            consumer.commitSync();
        } catch (WakeupException e) {
            doCommitSync();
            throw e;
        } catch (CommitFailedException e) {
            logger.error("Exception in consumer while doCommitSync", e);
        }
    }
}

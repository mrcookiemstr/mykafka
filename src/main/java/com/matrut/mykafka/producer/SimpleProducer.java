package com.matrut.mykafka.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimpleProducer implements Runnable {

    protected static final Logger logger = LogManager.getLogger();

    private KafkaProducer<String, String> kafkaProducer;
    private final String topic;
    private final AtomicBoolean shutdown;
    private final CountDownLatch shutdownLatch;

    public SimpleProducer(Properties config, String topic) {
        this.kafkaProducer = new KafkaProducer<>(config);
        this.topic = topic;
        this.shutdown = new AtomicBoolean(false);
        this.shutdownLatch = new CountDownLatch(1);
    }

    @Override
    public void run() {
        try {
            while (!shutdown.get()) {
                String key = String.format("example-key-%d", ThreadLocalRandom.current().nextInt());
                String value = String.format("some-value-%d", ThreadLocalRandom.current().nextInt());
                ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);

                kafkaProducer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        logger.error("Exception in producer while send", exception);
                    }

                    logger.debug("Message sent to topic: {} offset: {} partition: {} timestamp {}",
                            metadata.topic(),
                            metadata.offset(),
                            metadata.partition(),
                            metadata.timestamp());
                });

                Thread.sleep(1000);
            }
        } catch (InterruptedException exception) {
            // ignoring, we're closing
        } finally {
            kafkaProducer.close();
            shutdownLatch.countDown();
        }
    }

    public void shutdown() throws InterruptedException {
        shutdown.set(true);
        shutdownLatch.await();
    }
}

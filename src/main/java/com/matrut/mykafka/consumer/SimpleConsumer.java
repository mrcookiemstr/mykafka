package com.matrut.mykafka.consumer;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;

import java.time.Duration;
import java.util.Collection;
import java.util.Properties;
import java.util.List;

public class SimpleConsumer extends BaseConsumer implements Runnable {

    //private int failover = 0;

    public SimpleConsumer(Properties config, List<String> topics) {
        super(config, topics);
    }

    @Override
    public void run() {
        try {
            consumer.subscribe(topics, new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> collection) {
                    collection.forEach(partition -> {
                        logger.error("Consumer of topic {} partition {} was revoked ",
                                partition.topic(),
                                partition.partition());
                    });
                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> collection) {
                    collection.forEach(partition -> {
                        logger.error("Consumer of topic {} partition {} has been assigned",
                                partition.topic(),
                                partition.partition());
                    });
                }
            });

            while (!shutdown.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(3));
                records.forEach(this::process);


                /*failover++;

                if (failover > 5) {
                    throw new RuntimeException("Consumer failover");
                }*/

                doCommitSync();
            }
        } catch (WakeupException e) {
            // ignore, we're closing
        } catch (Exception e) {
            logger.error("Exception in consumer while receive", e);
        } finally {
            consumer.close();
            shutdownLatch.countDown();
        }
    }

    public void process(ConsumerRecord<String, String> record) {

        logger.debug("Simple consumer record for topic {} key {} value {} partition {} offset {}",
                record.topic(),
                record.key(),
                record.value(),
                record.partition(),
                record.offset());

    }
}

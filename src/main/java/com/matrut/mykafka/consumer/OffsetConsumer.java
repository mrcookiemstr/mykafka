package com.matrut.mykafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class OffsetConsumer extends BaseConsumer implements Runnable {
    public OffsetConsumer(Properties config, List<String> topics) {
        super(config, topics);
    }

    @Override
    public void run() {
        consumer.subscribe(topics);

        long oneHourEarlier = Instant.now()
                .atZone(ZoneId.systemDefault())
                .minusHours(1)
                .toEpochSecond();

        Map<TopicPartition, Long> partitionTimestampMap = consumer.assignment()
                .stream()
                .collect(Collectors.toMap(tp -> tp, tp -> oneHourEarlier));

        Map<TopicPartition, OffsetAndTimestamp> offsetMap = consumer.offsetsForTimes(partitionTimestampMap);

        for (Map.Entry<TopicPartition, OffsetAndTimestamp> entry : offsetMap.entrySet()) {
            consumer.seek(entry.getKey(), entry.getValue().offset());
        }

        try {
            while (!shutdown.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(3));
                records.forEach(this::process);

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

        logger.debug("Offset consumer record for topic {} key {} value {} partition {} offset {}",
                record.topic(),
                record.key(),
                record.value(),
                record.partition(),
                record.offset());

    }
}

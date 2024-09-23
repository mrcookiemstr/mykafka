package com.matrut.mykafka;

import com.matrut.mykafka.consumer.OffsetConsumer;
import com.matrut.mykafka.consumer.SimpleConsumer;
import com.matrut.mykafka.producer.SimpleProducer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Properties;

public class MyKafka {
    private static final String KAFKA_BROKERS = "localhost:8097,localhost:8098,localhost:8099";
    private static final String KAFKA_TOPIC = "my-topic";
    private static final String KAFKA_FIRST_CONSUMER_GROUP_ID = "first-consumer-myapp";
    private static final String KAFKA_SECOND_CONSUMER_GROUP_ID = "second-consumer-myapp";

    private static Properties buildProducerProps() {
        final Properties config = new Properties();
        config.put("client.id", buildClientId());
        config.put("bootstrap.servers", KAFKA_BROKERS);
        config.put("connections.max.idle.ms", 3000);
        config.put("request.timeout.ms", 3000);

        //
        config.put("enable.idempotence", "true");

        config.put("acks", "1");
        //config.put("acks", "all");

        config.put("partition.assignment.strategy", "RoundRobinAssignor");

        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        return config;
    }

    private static Properties buildConsumerProps(String groupId) {
        final Properties config = new Properties();
        config.put("client.id", buildClientId());
        config.put("bootstrap.servers", KAFKA_BROKERS);
        config.put("connections.max.idle.ms", 3000);
        config.put("request.timeout.ms", 3000);

        config.put("enable.auto.commit", false);
        //config.put("auto.commit.intervals.ms", "5000");
        //config.put("max.poll.records", "3");

        config.put("group.id", groupId);
        config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        return config;
    }

    private static String buildClientId() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] ars) {
        Thread producerThread = new Thread(new SimpleProducer(buildProducerProps(), KAFKA_TOPIC));
        Thread consumerThread = new Thread(new SimpleConsumer(buildConsumerProps(KAFKA_FIRST_CONSUMER_GROUP_ID), List.of(KAFKA_TOPIC)));
        //Thread consumerThread = new Thread(new OffsetConsumer(buildConsumerProps(KAFKA_SECOND_CONSUMER_GROUP_ID), List.of(KAFKA_TOPIC)));

        producerThread.start();
        consumerThread.start();

        try {
            producerThread.join();
            consumerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

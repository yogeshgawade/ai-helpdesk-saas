package com.helpdesk.redis;

import io.lettuce.core.RedisBusyException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;

@Configuration
public class RedisStreamConfig {

    public static final String TICKET_CLASSIFICATION_STREAM = "ticket.classification";
    public static final String TICKET_CLASSIFICATION_GROUP = "ticket-classification-workers";
    public static final String TICKET_CLASSIFICATION_DLQ = "ticket.classification.dlq";

    public static final String TICKET_SUMMARIZATION_STREAM = "ticket.summarization";
    public static final String TICKET_SUMMARIZATION_GROUP = "ticket-summarization-workers";
    public static final String TICKET_SUMMARIZATION_DLQ = "ticket.summarization.dlq";

    public static final int MAX_RETRIES = 3;

    public static final Duration PENDING_MESSAGE_IDLE_TIME =
            Duration.ofMinutes(5);

    public static final String CONSUMER_NAME =
            System.getenv().getOrDefault(
                    "HOSTNAME",
                    java.util.UUID.randomUUID().toString()
            );

    @Bean
    public RedisTemplate<String, String> redisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer serializer = new StringRedisSerializer();

        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public StreamMessageListenerContainer<String, org.springframework.data.redis.connection.stream.MapRecord<String, String, String>>
    streamMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<
                String,
                org.springframework.data.redis.connection.stream.MapRecord<String, String, String>
                > options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofSeconds(2))
                        .build();

        return StreamMessageListenerContainer.create(
                connectionFactory,
                options
        );
    }

    @Bean
    public boolean ticketClassificationConsumerGroup(
            RedisTemplate<String, String> redisTemplate) {

        try {
            redisTemplate.opsForStream().createGroup(
                    TICKET_CLASSIFICATION_STREAM,
                    ReadOffset.latest(),
                    TICKET_CLASSIFICATION_GROUP
            );
        } catch (RedisSystemException e) {

            Throwable cause = e;

            while (cause != null) {
                if (cause instanceof RedisBusyException) {
                    return true;
                }
                cause = cause.getCause();
            }

            throw e;
        }

        return true;
    }

    @Bean
    public boolean ticketSummarizationConsumerGroup(
            RedisTemplate<String, String> redisTemplate) {

        try {
            redisTemplate.opsForStream().createGroup(
                    TICKET_SUMMARIZATION_STREAM,
                    ReadOffset.latest(),
                    TICKET_SUMMARIZATION_GROUP
            );
        } catch (RedisSystemException e) {

            Throwable cause = e;

            while (cause != null) {
                if (cause instanceof RedisBusyException) {
                    return true;
                }
                cause = cause.getCause();
            }

            throw e;
        }

        return true;
    }
}

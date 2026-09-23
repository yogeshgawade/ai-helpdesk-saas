package com.helpdesk.redis;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TicketClassificationProducerTest {

    @Test
    void shouldPublishClassificationJob() {

        RedisTemplate<String, String> redisTemplate =
                mock(RedisTemplate.class);

        StreamOperations<String, Object, Object> streamOperations =
                mock(StreamOperations.class);

        when(redisTemplate.opsForStream())
                .thenReturn(streamOperations);

        TicketClassificationProducer producer =
                new TicketClassificationProducer(redisTemplate);

        String ticketId =
                "efc07661-5c7e-47ba-8555-23b99d94e24e";

        String organizationId =
                "8a568ac9-b1b2-41cc-a19a-86de79f83d4e";

        producer.publish(
                ticketId,
                organizationId,
                "Cannot access my account",
                "HIGH",
                "ACCOUNT"
        );

        verify(streamOperations).add(
                eq(RedisStreamConfig.TICKET_CLASSIFICATION_STREAM),
                eq(Map.of(
                        "ticketId", ticketId,
                        "organizationId", organizationId,
                        "subject", "Cannot access my account",
                        "priority", "HIGH",
                        "category", "ACCOUNT"
                ))
        );
    }

    @Test
    void shouldPublishWithoutCategoryWhenCategoryIsNull() {

        RedisTemplate<String, String> redisTemplate =
                mock(RedisTemplate.class);

        StreamOperations<String, Object, Object> streamOperations =
                mock(StreamOperations.class);

        when(redisTemplate.opsForStream())
                .thenReturn(streamOperations);

        TicketClassificationProducer producer =
                new TicketClassificationProducer(redisTemplate);

        String ticketId =
                "efc07661-5c7e-47ba-8555-23b99d94e24e";

        String organizationId =
                "8a568ac9-b1b2-41cc-a19a-86de79f83d4e";

        producer.publish(
                ticketId,
                organizationId,
                "Cannot access my account",
                "HIGH",
                null
        );

        verify(streamOperations).add(
                eq(RedisStreamConfig.TICKET_CLASSIFICATION_STREAM),
                eq(Map.of(
                        "ticketId", ticketId,
                        "organizationId", organizationId,
                        "subject", "Cannot access my account",
                        "priority", "HIGH"
                ))
        );
    }
}

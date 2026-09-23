package com.helpdesk.redis;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StreamOperations;

import java.time.Duration;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TicketSummarizationProducerTest {

    @Test
    void shouldPublishWhenNoQueuedMarkerExists() {

        RedisTemplate<String, String> redisTemplate =
                mock(RedisTemplate.class);

        ValueOperations<String, String> valueOperations =
                mock(ValueOperations.class);

        StreamOperations<String, Object, Object> streamOperations =
                mock(StreamOperations.class);

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        when(redisTemplate.opsForStream())
                .thenReturn(streamOperations);

        when(valueOperations.setIfAbsent(
                any(String.class),
                eq("1"),
                any(Duration.class)
        ))
                .thenReturn(true);

        TicketSummarizationProducer producer =
                new TicketSummarizationProducer(redisTemplate);

        String ticketId =
                "efc07661-5c7e-47ba-8555-23b99d94e24e";

        String organizationId =
                "8a568ac9-b1b2-41cc-a19a-86de79f83d4e";

        producer.publish(ticketId, organizationId);

        verify(streamOperations, times(1)).add(
                eq(RedisStreamConfig.TICKET_SUMMARIZATION_STREAM),
                any(Map.class)
        );
    }

    @Test
    void shouldPublishOnlyOnceWhileQueuedMarkerExists() {

        RedisTemplate<String, String> redisTemplate =
                mock(RedisTemplate.class);

        ValueOperations<String, String> valueOperations =
                mock(ValueOperations.class);

        StreamOperations<String, Object, Object> streamOperations =
                mock(StreamOperations.class);

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        when(redisTemplate.opsForStream())
                .thenReturn(streamOperations);

        when(valueOperations.setIfAbsent(
                any(String.class),
                eq("1"),
                any(Duration.class)
        ))
                .thenReturn(true)
                .thenReturn(false);

        TicketSummarizationProducer producer =
                new TicketSummarizationProducer(redisTemplate);

        String ticketId =
                "efc07661-5c7e-47ba-8555-23b99d94e24e";

        String organizationId =
                "8a568ac9-b1b2-41cc-a19a-86de79f83d4e";

        producer.publish(ticketId, organizationId);
        producer.publish(ticketId, organizationId);

        verify(streamOperations, times(1)).add(
                eq(RedisStreamConfig.TICKET_SUMMARIZATION_STREAM),
                any(Map.class)
        );

        verify(valueOperations, times(2)).setIfAbsent(
                eq("ticket:summarization:queued:" + ticketId),
                eq("1"),
                any(Duration.class)
        );
    }
}

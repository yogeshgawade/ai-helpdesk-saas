package com.helpdesk.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class TicketSummarizationProducer {

    private static final Duration QUEUED_MARKER_TTL =
            Duration.ofMinutes(10);

    private final RedisTemplate<String, String> redisTemplate;

    public TicketSummarizationProducer(
            RedisTemplate<String, String> redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    public void publish(
            String ticketId,
            String organizationId
    ) {

        String markerKey =
                "ticket:summarization:queued:" + ticketId;

        Boolean markerCreated =
                redisTemplate.opsForValue().setIfAbsent(
                        markerKey,
                        "1",
                        QUEUED_MARKER_TTL
                );

        if (!Boolean.TRUE.equals(markerCreated)) {
            return;
        }

        Map<String, String> message = new HashMap<>();

        message.put("ticketId", ticketId);
        message.put("organizationId", organizationId);

        redisTemplate.opsForStream().add(
                RedisStreamConfig.TICKET_SUMMARIZATION_STREAM,
                message
        );
    }
}

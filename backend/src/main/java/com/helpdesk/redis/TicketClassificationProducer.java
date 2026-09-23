package com.helpdesk.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TicketClassificationProducer {

    private final RedisTemplate<String, String> redisTemplate;

    public TicketClassificationProducer(
            RedisTemplate<String, String> redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    public void publish(
            String ticketId,
            String organizationId,
            String subject,
            String priority,
            String category
    ) {

        Map<String, String> message = new HashMap<>();

        message.put("ticketId", ticketId);
        message.put("organizationId", organizationId);
        message.put("subject", subject);
        message.put("priority", priority);

        if (category != null) {
            message.put("category", category);
        }

        redisTemplate.opsForStream().add(
                RedisStreamConfig.TICKET_CLASSIFICATION_STREAM,
                message
        );
    }
}

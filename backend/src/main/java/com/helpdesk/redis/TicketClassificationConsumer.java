package com.helpdesk.redis;

import com.helpdesk.ai.client.AiServiceClient;
import com.helpdesk.ai.client.dto.ClassificationResponse;
import com.helpdesk.tickets.repository.TicketRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class TicketClassificationConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(TicketClassificationConsumer.class);

    private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private final RedisTemplate<String, String> redisTemplate;
    private final AiServiceClient aiServiceClient;
    private final TicketRepository ticketRepository;


    public TicketClassificationConsumer(
            StreamMessageListenerContainer<String, MapRecord<String, String, String>> container,
            RedisTemplate<String, String> redisTemplate,
            AiServiceClient aiServiceClient,
            TicketRepository ticketRepository
    ) {
        this.container = container;
        this.redisTemplate = redisTemplate;
        this.aiServiceClient = aiServiceClient;
        this.ticketRepository = ticketRepository;
    }

    @PostConstruct
    public void start() {

        recoverPendingMessages();

        container.receive(
                Consumer.from(
                        RedisStreamConfig.TICKET_CLASSIFICATION_GROUP,
                        RedisStreamConfig.CONSUMER_NAME
                ),
                StreamOffset.create(
                        RedisStreamConfig.TICKET_CLASSIFICATION_STREAM,
                        ReadOffset.lastConsumed()
                ),
                this::processMessage
        );

        container.start();
    }

    private void recoverPendingMessages() {

        var pendingMessages =
                redisTemplate.opsForStream().pending(
                        RedisStreamConfig.TICKET_CLASSIFICATION_STREAM,
                        RedisStreamConfig.TICKET_CLASSIFICATION_GROUP,
                        org.springframework.data.domain.Range.unbounded(),
                        100,
                        RedisStreamConfig.PENDING_MESSAGE_IDLE_TIME
                );

        if (pendingMessages.isEmpty()) {
            return;
        }

        var messageIds = pendingMessages.stream()
                .map(message -> message.getId())
                .toArray(org.springframework.data.redis.connection.stream.RecordId[]::new);

        List<MapRecord<String, Object, Object>> claimedMessages =
                redisTemplate.opsForStream().claim(
                        RedisStreamConfig.TICKET_CLASSIFICATION_STREAM,
                        RedisStreamConfig.TICKET_CLASSIFICATION_GROUP,
                        RedisStreamConfig.CONSUMER_NAME,
                        RedisStreamConfig.PENDING_MESSAGE_IDLE_TIME,
                        messageIds
                );

        for (MapRecord<String, Object, Object> message : claimedMessages) {

            log.info(
                    "Recovering stale ticket classification job messageId={}",
                    message.getId().getValue()
            );

            processRecoveredMessage(message);
        }
    }

    private void processRecoveredMessage(
            MapRecord<String, Object, Object> message
    ) {

        Map<Object, Object> values = message.getValue();

        processValues(
                message.getId().getValue(),
                String.valueOf(values.get("ticketId")),
                String.valueOf(values.get("organizationId")),
                String.valueOf(values.get("subject")),
                String.valueOf(values.get("priority")),
                values.get("category") != null
                        ? String.valueOf(values.get("category"))
                        : null,
                values.get("retryCount") != null
                        ? Integer.parseInt(String.valueOf(values.get("retryCount")))
                        : 0
        );
    }

    private void processMessage(
            MapRecord<String, String, String> message
    ) {

        Map<String, String> values = message.getValue();

        processValues(
                message.getId().getValue(),
                values.get("ticketId"),
                values.get("organizationId"),
                values.get("subject"),
                values.get("priority"),
                values.get("category"),
                values.get("retryCount") != null
                        ? Integer.parseInt(values.get("retryCount"))
                        : 0
        );
    }

    private void processValues(
            String messageId,
            String ticketId,
            String organizationId,
            String subject,
            String priority,
            String category,
            int retryCount
    ) {

        log.info(
                "Received ticket classification job ticketId={} retryCount={}",
                ticketId,
                retryCount
        );

        try {

            ClassificationResponse result =
                    aiServiceClient.classifyTicket(
                            subject,
                            priority,
                            category
                    );

            log.info(
                    "AI classification completed ticketId={} category={} priority={} confidence={}",
                    ticketId,
                    result.category(),
                    result.priority(),
                    result.confidence()
            );

            int updatedRows = ticketRepository.updateAiClassification(
                    UUID.fromString(ticketId),
                    UUID.fromString(organizationId),
                    result.category(),
                    result.priority(),
                    result.confidence(),
                    result.reason(),
                    Instant.now()
            );

            if (updatedRows != 1) {
                throw new IllegalStateException(
                        "Could not persist AI classification for ticket: "
                                + ticketId
                );
            }

            log.info(
                    "Persisted AI classification ticketId={}",
                    ticketId
            );

            redisTemplate.opsForStream().acknowledge(
                    RedisStreamConfig.TICKET_CLASSIFICATION_STREAM,
                    RedisStreamConfig.TICKET_CLASSIFICATION_GROUP,
                    messageId
            );

            log.debug(
                    "Acknowledged ticket classification job messageId={}",
                    messageId
            );

        } catch (Exception exception) {

            int nextRetryCount = retryCount + 1;

            log.warn(
                    "Ticket classification failed ticketId={} retry={}/{}",
                    ticketId,
                    nextRetryCount,
                    RedisStreamConfig.MAX_RETRIES,
                    exception
            );

            if (nextRetryCount >= RedisStreamConfig.MAX_RETRIES) {

                moveToDeadLetterQueue(
                        messageId,
                        ticketId,
                        organizationId,
                        subject,
                        priority,
                        category,
                        nextRetryCount,
                        exception
                );

            } else {

                retryMessage(
                        messageId,
                        ticketId,
                        organizationId,
                        subject,
                        priority,
                        category,
                        nextRetryCount
                );
            }
        }
    }

    private void retryMessage(
            String messageId,
            String ticketId,
            String organizationId,
            String subject,
            String priority,
            String category,
            int retryCount
    ) {

        Map<String, String> values = new HashMap<>();

        values.put("ticketId", ticketId);
        values.put("organizationId", organizationId);
        values.put("subject", subject);
        values.put("priority", priority);

        if (category != null) {
            values.put("category", category);
        }

        values.put("retryCount", String.valueOf(retryCount));

        redisTemplate.opsForStream().add(
                RedisStreamConfig.TICKET_CLASSIFICATION_STREAM,
                values
        );

        redisTemplate.opsForStream().acknowledge(
                RedisStreamConfig.TICKET_CLASSIFICATION_STREAM,
                RedisStreamConfig.TICKET_CLASSIFICATION_GROUP,
                messageId
        );

        log.info(
                "Requeued ticket classification job ticketId={} retryCount={}",
                ticketId,
                retryCount
        );
    }

    private void moveToDeadLetterQueue(
            String messageId,
            String ticketId,
            String organizationId,
            String subject,
            String priority,
            String category,
            int retryCount,
            Exception exception
    ) {

        Map<String, String> values = new HashMap<>();

        values.put("ticketId", ticketId);
        values.put("organizationId", organizationId);
        values.put("subject", subject);
        values.put("priority", priority);
        values.put("retryCount", String.valueOf(retryCount));
        values.put("error", exception.getMessage() != null
                ? exception.getMessage()
                : exception.getClass().getSimpleName());

        if (category != null) {
            values.put("category", category);
        }

        redisTemplate.opsForStream().add(
                RedisStreamConfig.TICKET_CLASSIFICATION_DLQ,
                values
        );

        redisTemplate.opsForStream().acknowledge(
                RedisStreamConfig.TICKET_CLASSIFICATION_STREAM,
                RedisStreamConfig.TICKET_CLASSIFICATION_GROUP,
                messageId
        );

        log.error(
                "Moved ticket classification job to DLQ ticketId={} retryCount={}",
                ticketId,
                retryCount,
                exception
        );
    }
}

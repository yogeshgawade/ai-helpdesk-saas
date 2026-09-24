package com.helpdesk.redis;

import com.helpdesk.ai.client.AiServiceClient;
import com.helpdesk.ai.client.dto.SummarizationMessage;
import com.helpdesk.ai.client.dto.SummarizationResponse;
import com.helpdesk.orgs.TenantTransactionExecutor;
import com.helpdesk.tickets.entity.Ticket;
import com.helpdesk.tickets.entity.TicketMessage;
import com.helpdesk.tickets.repository.TicketMessageRepository;
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
public class TicketSummarizationConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(TicketSummarizationConsumer.class);

    private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private final RedisTemplate<String, String> redisTemplate;
    private final AiServiceClient aiServiceClient;
    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final TenantTransactionExecutor tenantTransactionExecutor;


    public TicketSummarizationConsumer(
            StreamMessageListenerContainer<String, MapRecord<String, String, String>> container,
            RedisTemplate<String, String> redisTemplate,
            AiServiceClient aiServiceClient,
            TicketRepository ticketRepository,
            TicketMessageRepository ticketMessageRepository,
            TenantTransactionExecutor tenantTransactionExecutor
    ) {
        this.container = container;
        this.redisTemplate = redisTemplate;
        this.aiServiceClient = aiServiceClient;
        this.ticketRepository = ticketRepository;
        this.ticketMessageRepository = ticketMessageRepository;
        this.tenantTransactionExecutor = tenantTransactionExecutor;
    }

    @PostConstruct
    public void start() {

        recoverPendingMessages();

        container.receive(
                Consumer.from(
                        RedisStreamConfig.TICKET_SUMMARIZATION_GROUP,
                        RedisStreamConfig.CONSUMER_NAME
                ),
                StreamOffset.create(
                        RedisStreamConfig.TICKET_SUMMARIZATION_STREAM,
                        ReadOffset.lastConsumed()
                ),
                this::processMessage
        );

        container.start();
    }

    private void recoverPendingMessages() {

        var pendingMessages =
                redisTemplate.opsForStream().pending(
                        RedisStreamConfig.TICKET_SUMMARIZATION_STREAM,
                        RedisStreamConfig.TICKET_SUMMARIZATION_GROUP,
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
                        RedisStreamConfig.TICKET_SUMMARIZATION_STREAM,
                        RedisStreamConfig.TICKET_SUMMARIZATION_GROUP,
                        RedisStreamConfig.CONSUMER_NAME,
                        RedisStreamConfig.PENDING_MESSAGE_IDLE_TIME,
                        messageIds
                );

        for (MapRecord<String, Object, Object> message : claimedMessages) {

            log.info(
                    "Recovering stale ticket summarization job messageId={}",
                    message.getId().getValue()
            );

            processRecoveredMessage(message);
        }
    }

    private void processRecoveredMessage(
            MapRecord<String, Object, Object> message
    ) {

        Map<Object, Object> values = message.getValue();

        releaseQueuedMarker(
                String.valueOf(values.get("ticketId"))
        );

        processValues(
                message.getId().getValue(),
                String.valueOf(values.get("ticketId")),
                String.valueOf(values.get("organizationId")),
                values.get("retryCount") != null
                        ? Integer.parseInt(String.valueOf(values.get("retryCount")))
                        : 0
        );
    }

    private void processMessage(
            MapRecord<String, String, String> message
    ) {

        Map<String, String> values = message.getValue();

        releaseQueuedMarker(values.get("ticketId"));

        processValues(
                message.getId().getValue(),
                values.get("ticketId"),
                values.get("organizationId"),
                values.get("retryCount") != null
                        ? Integer.parseInt(values.get("retryCount"))
                        : 0
        );
    }

    private void releaseQueuedMarker(String ticketId) {

        String markerKey =
                "ticket:summarization:queued:" + ticketId;

        redisTemplate.delete(markerKey);
    }

    private void processValues(
            String messageId,
            String ticketId,
            String organizationId,
            int retryCount
    ) {

        log.info(
                "Received ticket summarization job ticketId={} retryCount={}",
                ticketId,
                retryCount
        );

        try {

            UUID ticketUuid = UUID.fromString(ticketId);
            UUID organizationUuid = UUID.fromString(organizationId);

            TicketSummaryData summaryData =
                    tenantTransactionExecutor.execute(
                            organizationUuid,
                            () -> {

                                Ticket ticket = ticketRepository
                                        .findByIdAndOrganizationId(
                                                ticketUuid,
                                                organizationUuid
                                        )
                                        .orElseThrow(() -> new IllegalStateException(
                                                "Ticket not found: " + ticketId
                                        ));

                                List<TicketMessage> messages =
                                        ticketMessageRepository
                                                .findByTicketIdAndInternalNoteFalseOrderByCreatedAtAscIdAsc(
                                                        ticketUuid
                                                );

                                if (messages.isEmpty()) {
                                    return null;
                                }

                                List<SummarizationMessage> summarizationMessages =
                                        messages.stream()
                                                .map(message -> new SummarizationMessage(
                                                        message.getAuthorId()
                                                                .equals(ticket.getCustomerId())
                                                                ? "customer"
                                                                : "agent",
                                                        message.getBody()
                                                ))
                                                .toList();

                                return new TicketSummaryData(
                                        ticket.getSubject(),
                                        summarizationMessages
                                );
                            }
                    );

            if (summaryData == null) {

                log.info(
                        "No public messages found for ticketId={}; acknowledging job",
                        ticketId
                );

                acknowledge(messageId);
                return;
            }

            SummarizationResponse result =
                    aiServiceClient.summarizeTicket(
                            summaryData.subject(),
                            summaryData.messages()
                    );

            log.info(
                    "AI summarization completed ticketId={}",
                    ticketId
            );

            int updatedRows =
                    tenantTransactionExecutor.execute(
                            organizationUuid,
                            () -> ticketRepository.updateAiSummary(
                                    ticketUuid,
                                    organizationUuid,
                                    result.summary(),
                                    Instant.now()
                            )
                    );

            if (updatedRows != 1) {
                throw new IllegalStateException(
                        "Could not persist AI summary for ticket: "
                                + ticketId
                );
            }

            log.info(
                    "Persisted AI summary ticketId={}",
                    ticketId
            );

            acknowledge(messageId);

        } catch (Exception exception) {

            int nextRetryCount = retryCount + 1;

            log.warn(
                    "Ticket summarization failed ticketId={} retry={}/{}",
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
                        nextRetryCount,
                        exception
                );

            } else {

                retryMessage(
                        messageId,
                        ticketId,
                        organizationId,
                        nextRetryCount
                );
            }
        }
    }

    private record TicketSummaryData(
            String subject,
            List<SummarizationMessage> messages
    ) {
    }

    private void retryMessage(
            String messageId,
            String ticketId,
            String organizationId,
            int retryCount
    ) {

        String markerKey =
                "ticket:summarization:queued:" + ticketId;

        redisTemplate.opsForValue().set(
                markerKey,
                "1",
                java.time.Duration.ofMinutes(10)
        );

        Map<String, String> values = new HashMap<>();

        values.put("ticketId", ticketId);
        values.put("organizationId", organizationId);
        values.put("retryCount", String.valueOf(retryCount));

        redisTemplate.opsForStream().add(
                RedisStreamConfig.TICKET_SUMMARIZATION_STREAM,
                values
        );

        acknowledge(messageId);

        log.info(
                "Requeued ticket summarization job ticketId={} retryCount={}",
                ticketId,
                retryCount
        );
    }

    private void moveToDeadLetterQueue(
            String messageId,
            String ticketId,
            String organizationId,
            int retryCount,
            Exception exception
    ) {

        Map<String, String> values = new HashMap<>();

        values.put("ticketId", ticketId);
        values.put("organizationId", organizationId);
        values.put("retryCount", String.valueOf(retryCount));
        values.put(
                "error",
                exception.getMessage() != null
                        ? exception.getMessage()
                        : exception.getClass().getSimpleName()
        );

        redisTemplate.opsForStream().add(
                RedisStreamConfig.TICKET_SUMMARIZATION_DLQ,
                values
        );

        acknowledge(messageId);

        log.error(
                "Moved ticket summarization job to DLQ ticketId={} retryCount={}",
                ticketId,
                retryCount,
                exception
        );
    }

    private void acknowledge(String messageId) {

        redisTemplate.opsForStream().acknowledge(
                RedisStreamConfig.TICKET_SUMMARIZATION_STREAM,
                RedisStreamConfig.TICKET_SUMMARIZATION_GROUP,
                messageId
        );

        log.debug(
                "Acknowledged ticket summarization job messageId={}",
                messageId
        );
    }
}

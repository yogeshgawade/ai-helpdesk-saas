package com.helpdesk.redis;

import com.helpdesk.ai.client.AiServiceClient;
import com.helpdesk.ai.client.dto.SummarizationResponse;
import com.helpdesk.tickets.entity.Ticket;
import com.helpdesk.tickets.entity.TicketMessage;
import com.helpdesk.tickets.repository.TicketMessageRepository;
import com.helpdesk.tickets.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TicketSummarizationConsumerTest {

    @Test
    void shouldReleaseQueuedMarkerWhenJobIsClaimed() throws Exception {

        RedisTemplate<String, String> redisTemplate =
                mock(RedisTemplate.class);

        StreamOperations<String, Object, Object> streamOperations =
                mock(StreamOperations.class);

        when(redisTemplate.opsForStream())
                .thenReturn(streamOperations);

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                mock(StreamMessageListenerContainer.class);

        AiServiceClient aiServiceClient =
                mock(AiServiceClient.class);

        TicketRepository ticketRepository =
                mock(TicketRepository.class);

        TicketMessageRepository ticketMessageRepository =
                mock(TicketMessageRepository.class);

        TicketSummarizationConsumer consumer =
                new TicketSummarizationConsumer(
                        container,
                        redisTemplate,
                        aiServiceClient,
                        ticketRepository,
                        ticketMessageRepository
                );

        UUID ticketId =
                UUID.fromString(
                        "efc07661-5c7e-47ba-8555-23b99d94e24e"
                );

        UUID organizationId =
                UUID.fromString(
                        "8a568ac9-b1b2-41cc-a19a-86de79f83d4e"
                );

        Ticket ticket = mock(Ticket.class);

        UUID customerId = UUID.randomUUID();

        when(ticket.getCustomerId())
                .thenReturn(customerId);

        when(ticket.getSubject())
                .thenReturn("Test ticket");

        when(ticketRepository.findByIdAndOrganizationId(
                ticketId,
                organizationId
        )).thenReturn(Optional.of(ticket));

        TicketMessage message = mock(TicketMessage.class);

        when(message.getAuthorId())
                .thenReturn(customerId);

        when(message.getBody())
                .thenReturn("Test customer message.");

        when(ticketMessageRepository
                .findByTicketIdAndInternalNoteFalseOrderByCreatedAtAscIdAsc(
                        ticketId
                ))
                .thenReturn(List.of(message));

        when(aiServiceClient.summarizeTicket(
                anyString(),
                anyList()
        )).thenReturn(
                new SummarizationResponse(
                        "Test summary."
                )
        );

        when(ticketRepository.updateAiSummary(
                eq(ticketId),
                eq(organizationId),
                anyString(),
                any(Instant.class)
        )).thenReturn(1);

        MapRecord<String, String, String> record =
                mock(MapRecord.class);

        when(record.getValue()).thenReturn(
                java.util.Map.of(
                        "ticketId", ticketId.toString(),
                        "organizationId", organizationId.toString()
                )
        );

        when(record.getId()).thenReturn(
                org.springframework.data.redis.connection.stream.RecordId.of("1-0")
        );

        Method processMessage =
                TicketSummarizationConsumer.class.getDeclaredMethod(
                        "processMessage",
                        MapRecord.class
                );

        processMessage.setAccessible(true);
        processMessage.invoke(consumer, record);

        verify(redisTemplate).delete(
                "ticket:summarization:queued:" + ticketId
        );

        verify(streamOperations).acknowledge(
                RedisStreamConfig.TICKET_SUMMARIZATION_STREAM,
                RedisStreamConfig.TICKET_SUMMARIZATION_GROUP,
                "1-0"
        );
    }
    @Test
    void shouldRequeueFailedSummarizationJob() throws Exception {

        RedisTemplate<String, String> redisTemplate =
                mock(RedisTemplate.class);

        StreamOperations<String, Object, Object> streamOperations =
                mock(StreamOperations.class);

        when(redisTemplate.opsForStream())
                .thenReturn(streamOperations);

        ValueOperations<String, String> valueOperations =
                mock(ValueOperations.class);

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                mock(StreamMessageListenerContainer.class);

        AiServiceClient aiServiceClient =
                mock(AiServiceClient.class);

        TicketRepository ticketRepository =
                mock(TicketRepository.class);

        TicketMessageRepository ticketMessageRepository =
                mock(TicketMessageRepository.class);

        TicketSummarizationConsumer consumer =
                new TicketSummarizationConsumer(
                        container,
                        redisTemplate,
                        aiServiceClient,
                        ticketRepository,
                        ticketMessageRepository
                );

        UUID ticketId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Ticket ticket = mock(Ticket.class);

        when(ticket.getCustomerId()).thenReturn(customerId);
        when(ticket.getSubject()).thenReturn("Test ticket");

        when(ticketRepository.findByIdAndOrganizationId(
                ticketId,
                organizationId
        )).thenReturn(Optional.of(ticket));

        TicketMessage message = mock(TicketMessage.class);

        when(message.getAuthorId()).thenReturn(customerId);
        when(message.getBody()).thenReturn("Test customer message.");

        when(ticketMessageRepository
                .findByTicketIdAndInternalNoteFalseOrderByCreatedAtAscIdAsc(
                        ticketId
                ))
                .thenReturn(List.of(message));

        when(aiServiceClient.summarizeTicket(
                anyString(),
                anyList()
        )).thenThrow(new RuntimeException("AI service unavailable"));

        MapRecord<String, String, String> record =
                mock(MapRecord.class);

        when(record.getValue()).thenReturn(
                Map.of(
                        "ticketId", ticketId.toString(),
                        "organizationId", organizationId.toString()
                )
        );

        when(record.getId()).thenReturn(
                org.springframework.data.redis.connection.stream.RecordId.of("3-0")
        );

        Method processMessage =
                TicketSummarizationConsumer.class.getDeclaredMethod(
                        "processMessage",
                        MapRecord.class
                );

        processMessage.setAccessible(true);
        processMessage.invoke(consumer, record);

        verify(streamOperations).add(
                eq(RedisStreamConfig.TICKET_SUMMARIZATION_STREAM),
                eq(Map.of(
                        "ticketId", ticketId.toString(),
                        "organizationId", organizationId.toString(),
                        "retryCount", "1"
                ))
        );

        verify(streamOperations).acknowledge(
                RedisStreamConfig.TICKET_SUMMARIZATION_STREAM,
                RedisStreamConfig.TICKET_SUMMARIZATION_GROUP,
                "3-0"
        );
    }

    @Test
    void shouldMoveSummarizationJobToDlqAfterMaxRetries() throws Exception {

        RedisTemplate<String, String> redisTemplate =
                mock(RedisTemplate.class);

        StreamOperations<String, Object, Object> streamOperations =
                mock(StreamOperations.class);

        when(redisTemplate.opsForStream())
                .thenReturn(streamOperations);

        ValueOperations<String, String> valueOperations =
                mock(ValueOperations.class);

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                mock(StreamMessageListenerContainer.class);

        AiServiceClient aiServiceClient =
                mock(AiServiceClient.class);

        TicketRepository ticketRepository =
                mock(TicketRepository.class);

        TicketMessageRepository ticketMessageRepository =
                mock(TicketMessageRepository.class);

        TicketSummarizationConsumer consumer =
                new TicketSummarizationConsumer(
                        container,
                        redisTemplate,
                        aiServiceClient,
                        ticketRepository,
                        ticketMessageRepository
                );

        UUID ticketId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Ticket ticket = mock(Ticket.class);

        when(ticket.getCustomerId()).thenReturn(customerId);
        when(ticket.getSubject()).thenReturn("Test ticket");

        when(ticketRepository.findByIdAndOrganizationId(
                ticketId,
                organizationId
        )).thenReturn(Optional.of(ticket));

        TicketMessage message = mock(TicketMessage.class);

        when(message.getAuthorId()).thenReturn(customerId);
        when(message.getBody()).thenReturn("Test customer message.");

        when(ticketMessageRepository
                .findByTicketIdAndInternalNoteFalseOrderByCreatedAtAscIdAsc(
                        ticketId
                ))
                .thenReturn(List.of(message));

        when(aiServiceClient.summarizeTicket(
                anyString(),
                anyList()
        )).thenThrow(new RuntimeException("AI service unavailable"));

        MapRecord<String, String, String> record =
                mock(MapRecord.class);

        when(record.getValue()).thenReturn(
                Map.of(
                        "ticketId", ticketId.toString(),
                        "organizationId", organizationId.toString(),
                        "retryCount", "2"
                )
        );

        when(record.getId()).thenReturn(
                org.springframework.data.redis.connection.stream.RecordId.of("4-0")
        );

        Method processMessage =
                TicketSummarizationConsumer.class.getDeclaredMethod(
                        "processMessage",
                        MapRecord.class
                );

        processMessage.setAccessible(true);
        processMessage.invoke(consumer, record);

        verify(streamOperations).add(
                eq(RedisStreamConfig.TICKET_SUMMARIZATION_DLQ),
                argThat(values ->
                        values.get("ticketId").equals(ticketId.toString())
                                && values.get("organizationId").equals(organizationId.toString())
                                && values.get("retryCount").equals("3")
                                && values.get("error").equals("AI service unavailable")
                )
        );

        verify(streamOperations).acknowledge(
                RedisStreamConfig.TICKET_SUMMARIZATION_STREAM,
                RedisStreamConfig.TICKET_SUMMARIZATION_GROUP,
                "4-0"
        );
    }

}

package com.helpdesk.redis;

import com.helpdesk.ai.client.AiServiceClient;
import com.helpdesk.ai.client.dto.ClassificationResponse;
import com.helpdesk.orgs.TenantTransactionExecutor;
import com.helpdesk.tickets.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TicketClassificationConsumerTest {

    @Test
    void shouldPersistClassificationAndAcknowledgeJob() throws Exception {

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

        TenantTransactionExecutor tenantTransactionExecutor =
                mock(TenantTransactionExecutor.class);

        UUID ticketId =
                UUID.fromString(
                        "efc07661-5c7e-47ba-8555-23b99d94e24e"
                );

        UUID organizationId =
                UUID.fromString(
                        "8a568ac9-b1b2-41cc-a19a-86de79f83d4e"
                );

        when(tenantTransactionExecutor.execute(
                eq(organizationId),
                any(Supplier.class)
        )).thenAnswer(invocation ->
                ((Supplier<Integer>) invocation.getArgument(1)).get()
        );

        TicketClassificationConsumer consumer =
                new TicketClassificationConsumer(
                        container,
                        redisTemplate,
                        aiServiceClient,
                        ticketRepository,
                        tenantTransactionExecutor
                );

        when(aiServiceClient.classifyTicket(
                eq("Cannot access my account"),
                eq("HIGH"),
                eq("ACCOUNT")
        )).thenReturn(
                new ClassificationResponse(
                        "ACCOUNT",
                        "HIGH",
                        0.95,
                        "The customer cannot access their account."
                )
        );

        when(ticketRepository.updateAiClassification(
                eq(ticketId),
                eq(organizationId),
                eq("ACCOUNT"),
                eq("HIGH"),
                eq(0.95),
                eq("The customer cannot access their account."),
                any()
        )).thenReturn(1);

        MapRecord<String, String, String> record =
                mock(MapRecord.class);

        when(record.getValue()).thenReturn(
                Map.of(
                        "ticketId", ticketId.toString(),
                        "organizationId", organizationId.toString(),
                        "subject", "Cannot access my account",
                        "priority", "HIGH",
                        "category", "ACCOUNT"
                )
        );

        when(record.getId()).thenReturn(
                RecordId.of("1-0")
        );

        Method processMessage =
                TicketClassificationConsumer.class.getDeclaredMethod(
                        "processMessage",
                        MapRecord.class
                );

        processMessage.setAccessible(true);
        processMessage.invoke(consumer, record);

        verify(aiServiceClient).classifyTicket(
                "Cannot access my account",
                "HIGH",
                "ACCOUNT"
        );

        verify(ticketRepository).updateAiClassification(
                eq(ticketId),
                eq(organizationId),
                eq("ACCOUNT"),
                eq("HIGH"),
                eq(0.95),
                eq("The customer cannot access their account."),
                any()
        );

        verify(streamOperations).acknowledge(
                RedisStreamConfig.TICKET_CLASSIFICATION_STREAM,
                RedisStreamConfig.TICKET_CLASSIFICATION_GROUP,
                "1-0"
        );
    }
    @Test
    void shouldRequeueFailedClassificationJob() throws Exception {

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

        TenantTransactionExecutor tenantTransactionExecutor =
                mock(TenantTransactionExecutor.class);

        TicketClassificationConsumer consumer =
                new TicketClassificationConsumer(
                        container,
                        redisTemplate,
                        aiServiceClient,
                        ticketRepository,
                        tenantTransactionExecutor
                );

        UUID ticketId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        when(aiServiceClient.classifyTicket(
                eq("Cannot access my account"),
                eq("HIGH"),
                eq("ACCOUNT")
        )).thenThrow(new RuntimeException("AI service unavailable"));

        MapRecord<String, String, String> record =
                mock(MapRecord.class);

        when(record.getValue()).thenReturn(
                Map.of(
                        "ticketId", ticketId.toString(),
                        "organizationId", organizationId.toString(),
                        "subject", "Cannot access my account",
                        "priority", "HIGH",
                        "category", "ACCOUNT"
                )
        );

        when(record.getId()).thenReturn(
                org.springframework.data.redis.connection.stream.RecordId.of("1-0")
        );

        Method processMessage =
                TicketClassificationConsumer.class.getDeclaredMethod(
                        "processMessage",
                        MapRecord.class
                );

        processMessage.setAccessible(true);
        processMessage.invoke(consumer, record);

        verify(streamOperations).add(
                eq(RedisStreamConfig.TICKET_CLASSIFICATION_STREAM),
                eq(Map.of(
                        "ticketId", ticketId.toString(),
                        "organizationId", organizationId.toString(),
                        "subject", "Cannot access my account",
                        "priority", "HIGH",
                        "category", "ACCOUNT",
                        "retryCount", "1"
                ))
        );

        verify(streamOperations).acknowledge(
                RedisStreamConfig.TICKET_CLASSIFICATION_STREAM,
                RedisStreamConfig.TICKET_CLASSIFICATION_GROUP,
                "1-0"
        );
    }

    @Test
    void shouldMoveClassificationJobToDlqAfterMaxRetries() throws Exception {

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

        TenantTransactionExecutor tenantTransactionExecutor =
                mock(TenantTransactionExecutor.class);

        TicketClassificationConsumer consumer =
                new TicketClassificationConsumer(
                        container,
                        redisTemplate,
                        aiServiceClient,
                        ticketRepository,
                        tenantTransactionExecutor
                );

        UUID ticketId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        when(aiServiceClient.classifyTicket(
                eq("Cannot access my account"),
                eq("HIGH"),
                eq("ACCOUNT")
        )).thenThrow(new RuntimeException("AI service unavailable"));

        MapRecord<String, String, String> record =
                mock(MapRecord.class);

        when(record.getValue()).thenReturn(
                Map.of(
                        "ticketId", ticketId.toString(),
                        "organizationId", organizationId.toString(),
                        "subject", "Cannot access my account",
                        "priority", "HIGH",
                        "category", "ACCOUNT",
                        "retryCount", "2"
                )
        );

        when(record.getId()).thenReturn(
                org.springframework.data.redis.connection.stream.RecordId.of("2-0")
        );

        Method processMessage =
                TicketClassificationConsumer.class.getDeclaredMethod(
                        "processMessage",
                        MapRecord.class
                );

        processMessage.setAccessible(true);
        processMessage.invoke(consumer, record);

        verify(streamOperations).add(
                eq(RedisStreamConfig.TICKET_CLASSIFICATION_DLQ),
                argThat(values ->
                        values.get("ticketId").equals(ticketId.toString())
                                && values.get("organizationId").equals(organizationId.toString())
                                && values.get("retryCount").equals("3")
                                && values.get("error").equals("AI service unavailable")
                )
        );

        verify(streamOperations).acknowledge(
                RedisStreamConfig.TICKET_CLASSIFICATION_STREAM,
                RedisStreamConfig.TICKET_CLASSIFICATION_GROUP,
                "2-0"
        );
    }

}

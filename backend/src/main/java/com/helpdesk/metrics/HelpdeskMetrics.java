package com.helpdesk.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class HelpdeskMetrics {

    private final Counter ticketsCreated;
    private final Counter aiClassificationSuccess;
    private final Counter aiClassificationFailure;
    private final Counter aiClassificationDlq;
    private final Counter aiResponseGenerations;
    private final Timer aiResponseGenerationLatency;

    public HelpdeskMetrics(MeterRegistry meterRegistry) {
        this.ticketsCreated = Counter.builder("helpdesk.tickets.created")
                .description("Number of tickets created")
                .register(meterRegistry);

        this.aiClassificationSuccess = Counter.builder("helpdesk.ai.classification.success")
                .description("Number of successfully completed AI ticket classifications")
                .register(meterRegistry);

        this.aiClassificationFailure = Counter.builder("helpdesk.ai.classification.failure")
                .description("Number of failed AI ticket classification attempts")
                .register(meterRegistry);

        this.aiClassificationDlq = Counter.builder("helpdesk.ai.classification.dlq")
                .description("Number of ticket classification jobs moved to the dead-letter queue")
                .register(meterRegistry);

        this.aiResponseGenerations = Counter.builder("helpdesk.ai.response.generations")
                .description("Number of AI response suggestions successfully generated and persisted")
                .register(meterRegistry);

        this.aiResponseGenerationLatency = Timer.builder("helpdesk.ai.response.generation.latency")
                .description("AI response generation latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    public void incrementTicketsCreated() {
        ticketsCreated.increment();
    }

    public void incrementAiClassificationSuccess() {
        aiClassificationSuccess.increment();
    }

    public void incrementAiClassificationFailure() {
        aiClassificationFailure.increment();
    }

    public void incrementAiClassificationDlq() {
        aiClassificationDlq.increment();
    }

    public void recordAiResponseGeneration(Duration duration) {
        aiResponseGenerations.increment();
        aiResponseGenerationLatency.record(duration);
    }
}

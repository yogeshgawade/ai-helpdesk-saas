package com.helpdesk.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component("aiService")
public class AiServiceHealthIndicator implements HealthIndicator {

    private final HttpClient httpClient;
    private final String healthUrl;

    public AiServiceHealthIndicator(
            @Value("${app.ai-service.url}") String baseUrl) {

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        this.healthUrl = baseUrl + "/health";
    }

    @Override
    public Health health() {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(healthUrl))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() >= 200 &&
                    response.statusCode() < 300) {

                return Health.up()
                        .withDetail("service", "ai-service")
                        .build();
            }

            return Health.down()
                    .withDetail("service", "ai-service")
                    .withDetail("status", response.statusCode())
                    .build();

        } catch (Exception exception) {
            return Health.down()
                    .withDetail("service", "ai-service")
                    .withException(exception)
                    .build();
        }
    }
}

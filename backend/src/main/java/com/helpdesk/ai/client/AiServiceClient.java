package com.helpdesk.ai.client;

import com.helpdesk.ai.client.dto.DocumentProcessResponse;
import com.helpdesk.ai.client.dto.ClassificationRequest;
import com.helpdesk.ai.client.dto.ClassificationResponse;
import com.helpdesk.ai.client.dto.EmbeddingRequest;
import com.helpdesk.ai.client.dto.InsightRequest;
import com.helpdesk.ai.client.dto.InsightResponse;
import com.helpdesk.ai.client.dto.EmbeddingResponse;
import com.helpdesk.ai.client.dto.RagRequest;
import com.helpdesk.ai.client.dto.RagResponse;
import com.helpdesk.ai.client.dto.ResponseAssistantRequest;
import com.helpdesk.ai.client.dto.ResponseAssistantResponse;
import com.helpdesk.ai.client.dto.SummarizationMessage;
import com.helpdesk.ai.client.dto.SummarizationRequest;
import com.helpdesk.ai.client.dto.SummarizationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.helpdesk.web.CorrelationIdFilter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Component
public class AiServiceClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public AiServiceClient(
            ObjectMapper objectMapper,
            @Value("${app.ai-service.url}") String baseUrl
    ) {
    this.httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .build();
        this.objectMapper = objectMapper;
    this.baseUrl = baseUrl;
    }

    public ClassificationResponse classifyTicket(
            String subject,
            String priority,
            String category
    ) {
        String requestBody;

        try {
            requestBody = objectMapper.writeValueAsString(
                    new ClassificationRequest(
                            subject,
                            priority,
                            category
                    )
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Could not serialize classification request",
                    exception
            );
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/classify"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");

        addRequestIdHeader(requestBuilder);

        HttpRequest request = requestBuilder
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                requestBody,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8
                    )
            );

            if (response.statusCode() < 200 ||
                    response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "AI service returned HTTP " +
                        response.statusCode() +
                        ": " +
                        response.body()
                );
            }

            return objectMapper.readValue(
                    response.body(),
                    ClassificationResponse.class
            );

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not call AI service",
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "AI service call was interrupted",
                    exception
            );
        }
    }

    public SummarizationResponse summarizeTicket(
            String subject,
            List<SummarizationMessage> messages
    ) {
        String requestBody;

        try {
            requestBody = objectMapper.writeValueAsString(
                    new SummarizationRequest(
                            subject,
                            messages
                    )
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Could not serialize summarization request",
                    exception
            );
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/summarize"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");

        addRequestIdHeader(requestBuilder);

        HttpRequest request = requestBuilder
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                requestBody,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8
                    )
            );

            if (response.statusCode() < 200 ||
                    response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "AI service returned HTTP " +
                        response.statusCode() +
                        ": " +
                        response.body()
                );
            }

            return objectMapper.readValue(
                    response.body(),
                    SummarizationResponse.class
            );

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not call AI service",
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "AI service call was interrupted",
                    exception
            );
        }
    }

    public EmbeddingResponse createEmbedding(String text) {
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(new EmbeddingRequest(text));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize embedding request", exception);
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/embeddings"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");

        addRequestIdHeader(requestBuilder);

        HttpRequest request = requestBuilder
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "AI service returned HTTP " + response.statusCode() + ": " + response.body()
                );
            }
            return objectMapper.readValue(response.body(), EmbeddingResponse.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not call AI service", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI service call was interrupted", exception);
        }
    }

    public RagResponse generateRagResponse(RagRequest ragRequest) {
        String requestBody;

        try {
            requestBody = objectMapper.writeValueAsString(ragRequest);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Could not serialize RAG request",
                    exception
            );
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/rag/query"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");

        addRequestIdHeader(requestBuilder);

        HttpRequest request = requestBuilder
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                requestBody,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8
                    )
            );

            if (response.statusCode() < 200 ||
                    response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "AI service returned HTTP " +
                        response.statusCode() +
                        ": " +
                        response.body()
                );
            }

            return objectMapper.readValue(
                    response.body(),
                    RagResponse.class
            );

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not call AI service",
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "AI service call was interrupted",
                    exception
            );
        }
    }

    public InsightResponse generateInsight(InsightRequest requestBody) {
        String json;

        try {
            json = objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Could not serialize insight request",
                    exception
            );
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/insights"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");

        addRequestIdHeader(requestBuilder);

        HttpRequest request = requestBuilder
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                json,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8
                    )
            );

            if (response.statusCode() < 200 ||
                    response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "AI service returned HTTP " +
                        response.statusCode() +
                        ": " +
                        response.body()
                );
            }

            return objectMapper.readValue(
                    response.body(),
                    InsightResponse.class
            );

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not call AI insights service",
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "AI insights service call was interrupted",
                    exception
            );
        }
    }


    public DocumentProcessResponse processDocument(
            InputStream inputStream,
            String filename
    ) {
        String boundary = "----HelpdeskBoundary" + System.currentTimeMillis();

        try {
            byte[] fileBytes = inputStream.readAllBytes();

            String multipartPrefix =
                    "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"" +
                    filename + "\"\r\n" +
                    "Content-Type: application/octet-stream\r\n\r\n";

            String multipartSuffix =
                    "\r\n--" + boundary + "--\r\n";

            byte[] prefixBytes = multipartPrefix.getBytes(StandardCharsets.UTF_8);
            byte[] suffixBytes = multipartSuffix.getBytes(StandardCharsets.UTF_8);

            byte[] body = new byte[
                    prefixBytes.length +
                    fileBytes.length +
                    suffixBytes.length
            ];

            System.arraycopy(prefixBytes, 0, body, 0, prefixBytes.length);
            System.arraycopy(
                    fileBytes,
                    0,
                    body,
                    prefixBytes.length,
                    fileBytes.length
            );
            System.arraycopy(
                    suffixBytes,
                    0,
                    body,
                    prefixBytes.length + fileBytes.length,
                    suffixBytes.length
            );

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/documents/process"))
                    .header(
                            "Content-Type",
                            "multipart/form-data; boundary=" + boundary
                    )
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body));

            addRequestIdHeader(requestBuilder);

            HttpRequest request = requestBuilder.build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "AI service returned HTTP " +
                        response.statusCode() +
                        ": " +
                        response.body()
                );
            }

            return objectMapper.readValue(
                    response.body(),
                    DocumentProcessResponse.class
            );

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not call AI service",
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "AI service call was interrupted",
                    exception
            );
        }
    }

    public ResponseAssistantResponse generateResponseAssistant(
            ResponseAssistantRequest requestBody
    ) {
        String json;

        try {
            json = objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Could not serialize response assistant request",
                    exception
            );
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/response-assistant/suggest"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");

        addRequestIdHeader(requestBuilder);

        HttpRequest request = requestBuilder
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                json,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8
                    )
            );

            if (response.statusCode() < 200 ||
                    response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "AI service returned HTTP " +
                        response.statusCode() +
                        ": " +
                        response.body()
                );
            }

            return objectMapper.readValue(
                    response.body(),
                    ResponseAssistantResponse.class
            );

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not call AI response assistant",
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "AI response assistant call was interrupted",
                    exception
            );
        }
    }

    public String streamResponseAssistant(
            ResponseAssistantRequest requestBody,
            Consumer<String> onChunk
    ) {
        String json;

        try {
            json = objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Could not serialize response assistant request",
                    exception
            );
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/response-assistant/stream"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/x-ndjson");

        addRequestIdHeader(requestBuilder);

        HttpRequest request = requestBuilder
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                json,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream()
            );

            if (response.statusCode() < 200 ||
                    response.statusCode() >= 300) {

                String errorBody = new String(
                        response.body().readAllBytes(),
                        StandardCharsets.UTF_8
                );

                throw new IllegalStateException(
                        "AI service returned HTTP " +
                        response.statusCode() +
                        ": " +
                        errorBody
                );
            }

            String model = null;

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         response.body(),
                                         StandardCharsets.UTF_8
                                 )
                         )) {

                String line;

                while ((line = reader.readLine()) != null) {

                    if (line.isBlank()) {
                        continue;
                    }

                    JsonNode event = objectMapper.readTree(line);

                    String type = event.path("type").asText();

                    if ("chunk".equals(type)) {
                        String chunk = event.path("text").asText();

                        if (!chunk.isEmpty()) {
                            onChunk.accept(chunk);
                        }

                    } else if ("done".equals(type)) {
                        model = event.path("model").asText(null);

                    } else if ("error".equals(type)) {
                        throw new IllegalStateException(
                                event.path("message").asText(
                                        "AI streaming failed"
                                )
                        );
                    }
                }
            }

            if (model == null || model.isBlank()) {
                throw new IllegalStateException(
                        "AI streaming completed without model information"
                );
            }

            return model;

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not stream AI response",
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "AI response streaming was interrupted",
                    exception
            );
        }
    }

    private void addRequestIdHeader(HttpRequest.Builder requestBuilder) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return;
        }

        String requestId =
                (String) attributes.getRequest().getAttribute(
                        CorrelationIdFilter.ATTRIBUTE_NAME
                );

        if (requestId != null && !requestId.isBlank()) {
            requestBuilder.header(
                    CorrelationIdFilter.HEADER_NAME,
                    requestId
            );
        }
    }


}

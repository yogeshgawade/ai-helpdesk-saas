package com.helpdesk.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpdesk.auth.MembershipRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.UUID;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final MembershipRepository membershipRepository;
    private final ObjectMapper objectMapper;
        private final WebSocketSessionManager sessionManager;

    public WebSocketHandler(
            MembershipRepository membershipRepository,
                        ObjectMapper objectMapper,
                        WebSocketSessionManager sessionManager) {

        this.membershipRepository = membershipRepository;
        this.objectMapper = objectMapper;
                this.sessionManager = sessionManager;
    }

    @Override
    public void afterConnectionEstablished(
            WebSocketSession session) throws Exception {

        Object userId = session.getAttributes().get("userId");
        Object email = session.getAttributes().get("email");

        session.sendMessage(
                new TextMessage(
                        """
                        {
                          "type": "connected",
                          "message": "WebSocket connected",
                          "userId": "%s",
                          "email": "%s"
                        }
                        """.formatted(userId, email)
                )
        );
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message) throws Exception {

        JsonNode json = objectMapper.readTree(message.getPayload());

        String type = json.path("type").asText();

        if ("subscribe".equals(type)) {
            handleSubscribe(session, json);
            return;
        }

        sendError(session, "Unknown message type");
    }

    private void handleSubscribe(
            WebSocketSession session,
            JsonNode json) throws Exception {

        String organizationIdValue =
                json.path("organizationId").asText(null);

        if (organizationIdValue == null) {
            sendError(session, "organizationId is required");
            return;
        }

        UUID organizationId;

        try {
            organizationId = UUID.fromString(organizationIdValue);
        } catch (IllegalArgumentException e) {
            sendError(session, "Invalid organizationId");
            return;
        }

        Object userIdValue =
                session.getAttributes().get("userId");

        if (userIdValue == null) {
            sendError(session, "User is not authenticated");
            return;
        }

        UUID userId =
                UUID.fromString(userIdValue.toString());

        boolean isMember =
                membershipRepository
                        .existsByUserIdAndOrganizationId(
                                userId,
                                organizationId
                        );

        if (!isMember) {
            sendError(
                    session,
                    "You are not a member of this organization"
            );
            return;
        }

        Object previousOrganizationId =
                session.getAttributes().get("organizationId");

        if (previousOrganizationId != null) {
            sessionManager.unsubscribe(
                    UUID.fromString(previousOrganizationId.toString()),
                                        userId,
                    session
            );
        }

        session.getAttributes().put(
                "organizationId",
                organizationId
        );
        sessionManager.subscribe(
                organizationId,
                userId,
                session
        );

        session.sendMessage(
                new TextMessage(
                        """
                        {
                          "type": "subscribed",
                          "organizationId": "%s"
                        }
                        """.formatted(organizationId)
                )
        );
    }

        @Override
        public void afterConnectionClosed(
                        WebSocketSession session,
                        org.springframework.web.socket.CloseStatus status) {

                Object organizationId =
                                session.getAttributes().get("organizationId");
                Object userId =
                                session.getAttributes().get("userId");

                if (organizationId != null && userId != null) {
                        sessionManager.unsubscribe(
                                        UUID.fromString(organizationId.toString()),
                                        UUID.fromString(userId.toString()),
                                        session
                        );
                }
        }

    private void sendError(
            WebSocketSession session,
            String message) throws Exception {

        session.sendMessage(
                new TextMessage(
                        """
                        {
                          "type": "error",
                          "message": "%s"
                        }
                        """.formatted(message)
                )
        );
    }
}

package com.helpdesk.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {

    private final ConcurrentHashMap<UUID, Set<WebSocketSession>> sessionsByOrganization =
            new ConcurrentHashMap<>();

        private final ConcurrentHashMap<UUID, Set<WebSocketSession>> sessionsByUser =
            new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public WebSocketSessionManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

        public void subscribe(
            UUID organizationId,
            UUID userId,
            WebSocketSession session) {
        sessionsByOrganization
                .computeIfAbsent(
                        organizationId,
                        key -> ConcurrentHashMap.newKeySet()
                )
                .add(session);

        sessionsByUser
                .computeIfAbsent(
                        userId,
                        key -> ConcurrentHashMap.newKeySet()
                )
                .add(session);
    }

    public void unsubscribe(
            UUID organizationId,
            UUID userId,
            WebSocketSession session) {
        Set<WebSocketSession> organizationSessions =
                sessionsByOrganization.get(organizationId);

        if (organizationSessions != null) {
            organizationSessions.remove(session);

            if (organizationSessions.isEmpty()) {
                sessionsByOrganization.remove(organizationId);
            }
        }

        Set<WebSocketSession> userSessions =
                sessionsByUser.get(userId);

        if (userSessions != null) {
            userSessions.remove(session);

            if (userSessions.isEmpty()) {
                sessionsByUser.remove(userId);
            }
        }
    }

    public void broadcast(UUID organizationId, String message) {
        Set<WebSocketSession> sessions =
                sessionsByOrganization.get(organizationId);

        if (sessions == null) {
            return;
        }

        for (WebSocketSession session : sessions) {
            sendMessage(sessions, session, message);
        }
    }

    public void sendToUser(UUID userId, String message) {
        Set<WebSocketSession> sessions =
                sessionsByUser.get(userId);

        if (sessions == null) {
            return;
        }

        for (WebSocketSession session : sessions) {
            sendMessage(sessions, session, message);
        }
    }

    public void broadcastEvent(
            UUID organizationId,
            String type,
            Object data
        ) {

        try {
            String message = objectMapper.writeValueAsString(
                    new WebSocketEvent(
                            type,
                            organizationId,
                            data
                    )
            );

            broadcast(organizationId, message);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to broadcast WebSocket event",
                    e
            );
        }
    }

    public void sendUserEvent(
            UUID userId,
            UUID organizationId,
            String type,
            Object data) {

        try {
            String message = objectMapper.writeValueAsString(
                    new WebSocketEvent(
                            type,
                            organizationId,
                            data
                    )
            );

            sendToUser(userId, message);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to send user WebSocket event",
                    e
            );
        }
    }

    private void sendMessage(
            Set<WebSocketSession> sessions,
            WebSocketSession session,
            String message) {

        if (!session.isOpen()) {
            sessions.remove(session);
            return;
        }

        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            sessions.remove(session);
        }
    }
}

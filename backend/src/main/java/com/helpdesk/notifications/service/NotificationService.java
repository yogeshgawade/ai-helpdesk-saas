package com.helpdesk.notifications.service;

import com.helpdesk.notifications.entity.Notification;
import com.helpdesk.notifications.repository.NotificationRepository;
import com.helpdesk.websocket.WebSocketSessionManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
        private final WebSocketSessionManager webSocketSessionManager;

    public NotificationService(
                        NotificationRepository notificationRepository,
                        WebSocketSessionManager webSocketSessionManager) {

        this.notificationRepository = notificationRepository;
                this.webSocketSessionManager = webSocketSessionManager;
    }

    @Transactional
    public Notification createNotification(
            UUID userId,
            String type,
            Map<String, Object> payload) {

        Notification notification =
                new Notification(
                        userId,
                        type,
                        payload
                );

        Notification saved =
                notificationRepository.save(notification);

        UUID organizationId =
                UUID.fromString(
                        payload.get("organizationId").toString()
                );

        webSocketSessionManager.sendUserEvent(
                userId,
                organizationId,
                "notification.created",
                saved
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Notification> getNotifications(
            UUID userId,
            Pageable pageable) {

        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(
                        userId,
                        pageable
                );
    }

    @Transactional
    public void markAsRead(
            UUID notificationId,
            UUID userId) {

        Notification notification =
                notificationRepository
                        .findById(notificationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Notification not found"
                                )
                        );

        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException(
                    "Notification does not belong to user"
            );
        }

        notification.markAsRead();
    }
}
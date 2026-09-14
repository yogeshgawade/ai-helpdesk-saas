package com.helpdesk.notifications.controller;

import com.helpdesk.auth.JwtService;
import com.helpdesk.notifications.entity.Notification;
import com.helpdesk.notifications.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtService jwtService;

    public NotificationController(
            NotificationService notificationService,
            JwtService jwtService) {

        this.notificationService = notificationService;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<Page<Notification>> getNotifications(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = extractUserId(authorization);

        Pageable pageable =
                PageRequest.of(page, Math.min(size, 100));

        return ResponseEntity.ok(
                notificationService.getNotifications(
                        userId,
                        pageable
                )
        );
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @RequestHeader("Authorization") String authorization,
            @PathVariable UUID notificationId) {

        UUID userId = extractUserId(authorization);

        notificationService.markAsRead(
                notificationId,
                userId
        );

        return ResponseEntity.noContent().build();
    }

    private UUID extractUserId(String authorization) {

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            throw new IllegalArgumentException(
                    "Invalid Authorization header"
            );
        }

        String token =
                authorization.substring(7);

        return jwtService.extractUserId(token);
    }
}
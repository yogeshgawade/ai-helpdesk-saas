package com.helpdesk.websocket;

import com.helpdesk.auth.JwtService;
import com.helpdesk.auth.User;
import com.helpdesk.auth.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final Logger log =
            LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public WebSocketAuthInterceptor(
            JwtService jwtService,
            UserRepository userRepository) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        String query = request.getURI().getQuery();

        if (query == null || query.isBlank()) {
            return false;
        }

        String ticket = extractTicket(query);

        if (ticket == null ||
                !jwtService.isWebSocketTicketValid(ticket)) {
            log.debug("Rejected WebSocket handshake with invalid ticket");
            return false;
        }

        UUID userId = jwtService.extractUserId(ticket);

        User user =
                userRepository.findById(userId).orElse(null);

        if (user == null) {
            log.debug("Rejected WebSocket handshake for unknown user");
            return false;
        }

        attributes.put("userId", user.getId());
        attributes.put("email", user.getEmail());
        attributes.put("user", user);

        log.debug("WebSocket handshake accepted");
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
    }

    private String extractTicket(String query) {

        for (String parameter : query.split("&")) {

            String[] parts =
                    parameter.split("=", 2);

            if (parts.length == 2 &&
                    parts[0].equals("ticket")) {

                return URLDecoder.decode(
                        parts[1],
                        StandardCharsets.UTF_8
                );
            }
        }

        return null;
    }
}

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

        if (request.getURI().getQuery() == null) {
            return false;
        }

        String token =
                extractToken(request.getURI().getQuery());

        if (token == null || !jwtService.isTokenValid(token)) {
            return false;
        }

        UUID userId =
                jwtService.extractUserId(token);

        User user =
                userRepository.findById(userId).orElse(null);

        if (user == null) {
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

    private String extractToken(String query) {

        for (String parameter : query.split("&")) {

            String[] parts =
                    parameter.split("=", 2);

            if (parts.length == 2
                    && parts[0].equals("token")) {

                return parts[1];
            }
        }

        return null;
    }
}

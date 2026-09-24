package com.helpdesk.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler webSocketHandler;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    private final String allowedOrigin;

    public WebSocketConfig(
            WebSocketHandler webSocketHandler,
            WebSocketAuthInterceptor webSocketAuthInterceptor,
            @Value("${app.cors.allowed-origin}") String allowedOrigin) {

        this.webSocketHandler = webSocketHandler;
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    public void registerWebSocketHandlers(
            WebSocketHandlerRegistry registry) {

        registry
                .addHandler(webSocketHandler, "/ws")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOrigins(allowedOrigin);
    }
}

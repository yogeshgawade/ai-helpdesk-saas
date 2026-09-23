package com.helpdesk.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler webSocketHandler;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    public WebSocketConfig(
            WebSocketHandler webSocketHandler,
            WebSocketAuthInterceptor webSocketAuthInterceptor) {

        this.webSocketHandler = webSocketHandler;
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(
            WebSocketHandlerRegistry registry) {

        registry
                .addHandler(webSocketHandler, "/ws")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOrigins("http://localhost:5173");
    }
}

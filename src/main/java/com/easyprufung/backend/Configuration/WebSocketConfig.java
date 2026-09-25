package com.easyprufung.backend.Configuration;

import com.easyprufung.backend.Project.WebSocket.LandingPageWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private LandingPageWebSocketHandler landingPageWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(landingPageWebSocketHandler, "/ws/landingpage/stream")
                .setAllowedOrigins("*"); // For dev, restrict in prod!
    }
}
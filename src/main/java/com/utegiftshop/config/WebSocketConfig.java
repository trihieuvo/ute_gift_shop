package com.utegiftshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import com.utegiftshop.controller.VendorChatWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private VendorChatWebSocketHandler vendorChatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(vendorChatWebSocketHandler, "/ws/vendor/chat")
                .setAllowedOriginPatterns("*") // Use setAllowedOriginPatterns instead of setAllowedOrigins
                .withSockJS()
                .setHeartbeatTime(25000) // Send heartbeat every 25 seconds
                .setDisconnectDelay(5000); // Wait 5 seconds before closing on disconnect
    }
    
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(8192);
        container.setMaxSessionIdleTimeout(300000L); // 5 minutes timeout
        container.setAsyncSendTimeout(5000L); // 5 seconds send timeout
        return container;
    }
}
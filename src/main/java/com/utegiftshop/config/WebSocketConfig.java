package com.utegiftshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import com.utegiftshop.controller.ChatWebSocketHandler; // THÊM IMPORT NÀY
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {



    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // THAY ĐỔI ENDPOINT VÀ HANDLER
        registry.addHandler(chatWebSocketHandler, "/ws/chat") // <-- Đảm bảo biến này được khai báo và @Autowired ở trên
                .setAllowedOriginPatterns("*") // Use setAllowedOriginPatterns
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
package com.utegiftshop.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import com.utegiftshop.controller.VendorChatWebSocketHandler;
import com.utegiftshop.entity.User;
import com.utegiftshop.repository.UserRepository;
import com.utegiftshop.security.jwt.JwtTokenProvider;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private VendorChatWebSocketHandler vendorChatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(vendorChatWebSocketHandler, "/ws/vendor/chat")
                .addInterceptors(jwtAuthHandshakeInterceptor()) // Sử dụng interceptor mới
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Bean
    public HandshakeInterceptor jwtAuthHandshakeInterceptor() {
        return new HandshakeInterceptor() {
            @Autowired
            private JwtTokenProvider tokenProvider;
            @Autowired
            private UserRepository userRepository;

            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                           WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                if (request instanceof ServletServerHttpRequest) {
                    ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                    String jwt = servletRequest.getServletRequest().getParameter("token");

                    if (StringUtils.hasText(jwt) && tokenProvider.validateJwtToken(jwt)) {
                        try {
                            String email = tokenProvider.getEmailFromJwtToken(jwt);
                            User user = userRepository.findByEmail(email).orElse(null);

                            if (user != null) {
                                attributes.put("userId", user.getId());
                                attributes.put("userRole", user.getRole().getName());
                                attributes.put("userFullName", user.getFullName());
                                return true; // Cho phép kết nối
                            }
                        } catch (Exception e) {
                            // Log a more specific error
                            System.err.println("WebSocket Handshake Error: " + e.getMessage());
                        }
                    }
                }
                return false; // Từ chối kết nối nếu không xác thực được
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Exception exception) {
                // No-op
            }
        };
    }


    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(16384);
        container.setMaxBinaryMessageBufferSize(16384);
        return container;
    }
}
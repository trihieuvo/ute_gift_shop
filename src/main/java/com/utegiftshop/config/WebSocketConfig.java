package com.utegiftshop.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils; // Thêm import này
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor; // Thêm import này

import java.util.Map;

import com.utegiftshop.controller.VendorChatWebSocketHandler;
import com.utegiftshop.security.jwt.JwtTokenProvider; // Import JwtTokenProvider
import com.utegiftshop.security.service.UserDetailsImpl; // Import UserDetailsImpl
import com.utegiftshop.security.service.UserDetailsServiceImpl; // Import UserDetailsServiceImpl
import com.utegiftshop.entity.User; // Import User
import com.utegiftshop.repository.UserRepository; // Import UserRepository

import jakarta.servlet.http.HttpSession; // Import HttpSession

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private VendorChatWebSocketHandler vendorChatWebSocketHandler;

    @Autowired
    private JwtTokenProvider tokenProvider; // Tiêm JwtTokenProvider

    @Autowired
    private UserDetailsServiceImpl userDetailsService; // Tiêm UserDetailsService

    @Autowired
    private UserRepository userRepository; // Tiêm UserRepository

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(vendorChatWebSocketHandler, "/ws/vendor/chat") // Endpoint
                .addInterceptors(httpSessionHandshakeInterceptor()) // Thêm interceptor để lấy userId
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    // Bean này giúp lấy thông tin HTTP Session (nếu cần) và truyền attributes
    @Bean
    public HandshakeInterceptor httpSessionHandshakeInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                           WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                if (request instanceof ServletServerHttpRequest) {
                    ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                    // 1. Cố gắng lấy token từ Query Parameter (phổ biến với SockJS)
                    String jwt = servletRequest.getServletRequest().getParameter("token");

                    // 2. Hoặc thử lấy từ Header (ít phổ biến hơn với WebSocket handshake qua JS thuần)
                    if (!StringUtils.hasText(jwt)) {
                         String headerAuth = servletRequest.getServletRequest().getHeader("Authorization");
                         if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
                            jwt = headerAuth.substring(7);
                         }
                    }

                    System.out.println("WebSocket Handshake: Attempting to process token: " + (jwt != null ? jwt.substring(0, 10) + "..." : "null"));


                    if (StringUtils.hasText(jwt) && tokenProvider.validateJwtToken(jwt)) {
                        try {
                            String email = tokenProvider.getEmailFromJwtToken(jwt);
                            // Dùng UserRepository để lấy User entity đầy đủ (bao gồm ID)
                             User user = userRepository.findByEmail(email).orElse(null);

                            if (user != null) {
                                // Lưu userId và role vào WebSocket session attributes
                                attributes.put("userId", user.getId());
                                attributes.put("userRole", user.getRole().getName());
                                attributes.put("userFullName", user.getFullName()); // Thêm tên để hiển thị
                                System.out.println("WebSocket Handshake: Authenticated user ID " + user.getId() + " Role: " + user.getRole().getName());
                                return true; // Cho phép kết nối
                            } else {
                                System.out.println("WebSocket Handshake: User not found in DB for email: " + email);
                            }
                        } catch (Exception e) {
                             System.err.println("WebSocket Handshake: Error processing token: " + e.getMessage());
                        }
                    } else {
                         System.out.println("WebSocket Handshake: Invalid or missing token.");
                    }
                }
                 System.out.println("WebSocket Handshake: Denying connection.");
                return false; // Từ chối kết nối nếu không xác thực được
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Exception exception) {
                // Không cần làm gì sau handshake ở đây
            }
        };
    }


    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(16384); // Tăng buffer size nếu cần
        container.setMaxBinaryMessageBufferSize(16384);
        // Tạm thời giữ timeout mặc định
        // container.setMaxSessionIdleTimeout(300000L);
        // container.setAsyncSendTimeout(5000L);
        return container;
    }
}
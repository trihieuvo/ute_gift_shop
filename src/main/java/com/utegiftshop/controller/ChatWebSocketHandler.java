package com.utegiftshop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.utegiftshop.entity.ChatMessage;
import com.utegiftshop.repository.ChatMessageRepository;
import com.utegiftshop.security.service.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    
    // Dùng Map để lưu {UserId -> WebSocketSession}
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            logger.warn("WebSocket connection established but user ID is null. Closing session.");
            session.close(CloseStatus.POLICY_VIOLATION.withReason("User not authenticated"));
            return;
        }
        
        userSessions.put(userId, session);
        logger.info("WebSocket connection established for User ID: {}. Session ID: {}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long senderId = getUserIdFromSession(session);
        if (senderId == null) {
            logger.warn("Received message from unauthenticated session {}. Ignoring.", session.getId());
            return;
        }

        try {
            // 1. Đọc payload (dữ liệu gửi lên)
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            Long receiverId = Long.parseLong(payload.get("receiverId").toString());
            String content = (String) payload.get("content");
            String senderRole = getRoleFromSession(session);
            
            if (content == null || content.trim().isEmpty() || receiverId == null) {
                logger.warn("Invalid message payload from sender {}. Missing content or receiverId.", senderId);
                return;
            }

            // 2. Tạo Conversation ID (đảm bảo nhất quán)
            String conversationId = generateConversationId(senderId, receiverId, senderRole);

            // 3. Tạo và Lưu Entity
            ChatMessage chatMessage = new ChatMessage(
                senderId, 
                receiverId, 
                senderRole, 
                content, 
                conversationId
            );
            chatMessage.setTimestamp(LocalDateTime.now());
            chatMessage.setCreatedAt(LocalDateTime.now());
            chatMessage.setIsRead(false);

            ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
            String messageJson = objectMapper.writeValueAsString(savedMessage);

            // 4. Gửi tin nhắn cho người nhận (nếu họ online)
            WebSocketSession receiverSession = userSessions.get(receiverId);
            if (receiverSession != null && receiverSession.isOpen()) {
                try {
                    receiverSession.sendMessage(new TextMessage(messageJson));
                    // Đánh dấu đã đọc ngay lập tức nếu người nhận đang online
                    // (Trong ứng dụng thực tế, bạn nên đợi tín hiệu "đã xem" từ client)
                    savedMessage.setIsRead(true); 
                    chatMessageRepository.save(savedMessage); // Cập nhật lại
                } catch (IOException e) {
                    logger.error("Failed to send message to receiver {}: {}", receiverId, e.getMessage());
                }
            }

            // 5. Gửi lại tin nhắn cho chính người gửi (để UI của họ cập nhật)
            session.sendMessage(new TextMessage(messageJson));

        } catch (Exception e) {
            logger.error("Error handling WebSocket message from sender {}: {}", senderId, e.getMessage(), e);
            session.sendMessage(new TextMessage("{\"error\":\"Lỗi xử lý tin nhắn.\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.remove(userId);
            logger.info("WebSocket connection closed for User ID: {}. Status: {}", userId, status);
        } else {
             logger.info("WebSocket connection closed for unauthenticated session {}. Status: {}", session.getId(), status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
    }

    // === Helper Functions ===

    private Long getUserIdFromSession(WebSocketSession session) {
        Principal principal = session.getPrincipal();
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            Object userDetails = ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
            if (userDetails instanceof UserDetailsImpl) {
                return ((UserDetailsImpl) userDetails).getId();
            }
        }
        // Fallback: Thử lấy từ URI (nếu dùng query param, nhưng SockJS Handshake không hỗ trợ)
        // Logic này cần được xem xét kỹ lưỡng nếu dùng Spring Security
        logger.warn("Could not extract UserDetailsImpl from session principal.");
        return null;
    }
    
    private String getRoleFromSession(WebSocketSession session) {
         Principal principal = session.getPrincipal();
         if (principal instanceof UsernamePasswordAuthenticationToken) {
             return ((UsernamePasswordAuthenticationToken) principal).getAuthorities().stream()
                 .findFirst()
                 .map(Object::toString)
                 .orElse("Customer");
         }
         return "Customer";
    }

    private String generateConversationId(Long senderId, Long receiverId, String senderRole) {
        // ID cuộc trò chuyện luôn là "vendor_{vendorId}_customer_{customerId}"
        if ("Vendor".equalsIgnoreCase(senderRole)) {
            // Người gửi là Vendor
            return "vendor_" + senderId + "_customer_" + receiverId;
        } else {
            // Người gửi là Customer
            return "vendor_" + receiverId + "_customer_" + senderId;
        }
    }
}
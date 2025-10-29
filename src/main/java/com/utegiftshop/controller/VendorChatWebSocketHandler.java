package com.utegiftshop.controller;

 import com.fasterxml.jackson.databind.ObjectMapper;
 import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
 import com.utegiftshop.entity.ChatMessage;
 import com.utegiftshop.repository.ChatMessageRepository;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.stereotype.Component;
 import org.springframework.web.socket.CloseStatus;
 import org.springframework.web.socket.TextMessage;
 import org.springframework.web.socket.WebSocketSession;
 import org.springframework.web.socket.handler.TextWebSocketHandler;
 import org.springframework.util.StringUtils;

 import jakarta.annotation.PostConstruct;
 import java.io.IOException;
 import java.time.LocalDateTime;
 import java.util.HashMap; // <-- Import HashMap
 import java.util.List;
 import java.util.Map;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.CopyOnWriteArrayList;

 @Component
 public class VendorChatWebSocketHandler extends TextWebSocketHandler {

     private static final Logger logger = LoggerFactory.getLogger(VendorChatWebSocketHandler.class);
     private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
     private final Map<Long, WebSocketSession> userSessionMap = new ConcurrentHashMap<>();
     private final ObjectMapper objectMapper = new ObjectMapper();

     @Autowired
     private ChatMessageRepository chatMessageRepository;

     @PostConstruct
     public void init() {
         objectMapper.registerModule(new JavaTimeModule());
     }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        String userFullName = (String) session.getAttributes().get("userFullName");
        String sessionId = session.getId(); // Get session ID for logging

        if (userId == null) {
            logger.warn("User ID not found in session attributes, closing connection: {}", sessionId);
            try {
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Authentication required"));
            } catch (IOException e) {
                logger.error("Error closing unauthenticated session {}: {}", sessionId, e.getMessage());
            }
            return;
        }

        sessions.add(session);
        userSessionMap.put(userId, session);
        logger.info("WebSocket connection established: Session ID {}, User ID {} ({}), Total sessions: {}, User Map Size: {}",
                sessionId, userId, userFullName, sessions.size(), userSessionMap.size());

        // === MODIFICATION START ===
        try {
            // Use a mutable HashMap
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("type", "SYSTEM");
            systemMessage.put("content", "Kết nối chat thành công!");
            systemMessage.put("userId", userId);
            // The sendMessage helper will add the timestamp

            logger.debug("Attempting to send initial SYSTEM message to Session ID {}", sessionId);
            sendMessage(session, systemMessage); // Send the message
            logger.info("Successfully sent initial SYSTEM message to Session ID {}", sessionId);

        } catch (Exception e) {
            // Log any error during the initial message send
            logger.error("Error sending initial SYSTEM message to Session ID {}: {}", sessionId, e.getMessage(), e);
            // Optionally close the connection if the initial message fails,
            // as it might indicate a problem.
            // try {
            //     session.close(CloseStatus.SERVER_ERROR.withReason("Failed to initialize session"));
            // } catch (IOException closeEx) {
            //     logger.error("Error closing session {} after initial message failure: {}", sessionId, closeEx.getMessage());
            // }
        }
        // === MODIFICATION END ===
    }

     // ... (handleTextMessage remains the same) ...
     @Override
     protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
         Long currentUserId = (Long) session.getAttributes().get("userId");
         if (currentUserId == null) {
              logger.warn("Received message from unauthenticated session: {}", session.getId());
              sendMessage(session, Map.of("type", "ERROR", "content", "Phiên không hợp lệ, vui lòng kết nối lại."));
              session.close();
              return;
         }

         String payload = message.getPayload();
         logger.info("Received message from User ID {}: {}", currentUserId, payload);

         try {
             Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);
             String type = (String) messageData.getOrDefault("type", "MESSAGE");

             if ("PING".equals(type)) {
                 sendMessage(session, Map.of("type", "PONG"));
                 return;
             }

             if ("MESSAGE".equals(type)) {
                 Long senderId = currentUserId;
                 Long receiverId = parseLong(messageData.get("receiverId"));
                 String senderRole = (String) session.getAttributes().get("userRole");
                 String content = (String) messageData.get("content");

                 if (receiverId == null || !StringUtils.hasText(senderRole) || !StringUtils.hasText(content)) {
                     logger.warn("Received incomplete message data from User ID {}: {}", currentUserId, messageData);
                     sendMessage(session, Map.of("type", "ERROR", "content", "Thiếu thông tin người nhận hoặc nội dung."));
                     return;
                 }

                 String conversationId = generateConversationId(senderId, receiverId);

                 ChatMessage chatMessage = new ChatMessage();
                 chatMessage.setSenderId(senderId);
                 chatMessage.setReceiverId(receiverId);
                 chatMessage.setSenderRole(senderRole);
                 chatMessage.setContent(content);
                 chatMessage.setConversationId(conversationId);
                 // createdAt will be set by @PrePersist

                 ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
                 logger.info("Message saved to DB with ID: {}", savedMessage.getId());

                 Map<String, Object> messageToSend = new ConcurrentHashMap<>(messageData); // Use ConcurrentHashMap for thread safety if needed
                 messageToSend.put("messageId", savedMessage.getId());
                 messageToSend.put("conversationId", conversationId);
                 messageToSend.put("senderId", senderId);
                 messageToSend.put("senderRole", senderRole);
                 messageToSend.put("timestamp", savedMessage.getCreatedAt()); // Use createdAt from DB
                 messageToSend.put("senderName", (String) session.getAttributes().get("userFullName"));

                 sendToRelevantUsers(messageToSend);
             }

         } catch (Exception e) {
             logger.error("Error processing message from User ID {}: {}", currentUserId, e.getMessage(), e);
             sendMessage(session, Map.of("type", "ERROR", "content", "Lỗi xử lý tin nhắn: " + e.getMessage()));
         }
     }


     // ... (sendToRelevantUsers, parseLong, generateConversationId, sendMessage remain the same) ...
     private void sendToRelevantUsers(Map<String, Object> messageData) {
         Long senderId = parseLong(messageData.get("senderId"));
         Long receiverId = parseLong(messageData.get("receiverId"));

         if (senderId == null || receiverId == null) {
             logger.error("Cannot send message, senderId or receiverId is null.");
             return;
         }

         WebSocketSession receiverSession = userSessionMap.get(receiverId);
         if (receiverSession != null && receiverSession.isOpen()) {
             try {
                 sendMessage(receiverSession, messageData);
                 logger.info("Message sent to receiver User ID {}", receiverId);
             } catch (IOException e) {
                 logger.error("Error sending message to receiver User ID {}: {}", receiverId, e.getMessage());
             }
         } else {
              logger.info("Receiver User ID {} is not connected.", receiverId);
         }

         WebSocketSession senderSession = userSessionMap.get(senderId);
         if (senderSession != null && senderSession.isOpen()) {
              try {
                 sendMessage(senderSession, messageData);
                 logger.info("Message echoed back to sender User ID {}", senderId);
             } catch (IOException e) {
                 logger.error("Error sending message back to sender User ID {}: {}", senderId, e.getMessage());
             }
         }
     }

     private Long parseLong(Object obj) {
         if (obj == null) return null;
         if (obj instanceof Number) return ((Number) obj).longValue();
         try {
             return Long.parseLong(obj.toString());
         } catch (NumberFormatException e) {
             logger.error("Failed to parse Long from object: {}", obj, e);
             return null;
         }
     }

     private String generateConversationId(Long userId1, Long userId2) {
         if (userId1 == null || userId2 == null) return null;
         long u1 = Math.min(userId1, userId2);
         long u2 = Math.max(userId1, userId2);
         return "conv_" + u1 + "_" + u2;
     }

     private void sendMessage(WebSocketSession session, Map<String, Object> data) throws IOException {
         if (session != null && session.isOpen()) {
             // Ensure timestamp exists, prefer existing one if present
             data.computeIfAbsent("timestamp", k -> LocalDateTime.now());

             String jsonMessage = objectMapper.writeValueAsString(data);
             synchronized (session) { // Keep synchronized for safety with potential concurrent access
                  session.sendMessage(new TextMessage(jsonMessage));
             }
         } else {
             logger.warn("Attempted to send message to a closed or null session (ID: {}).", session != null ? session.getId() : "null");
         }
     }


     @Override
     public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
         Long userId = (Long) session.getAttributes().get("userId");
         sessions.remove(session);
         if (userId != null) {
             userSessionMap.remove(userId);
         }
         logger.info("WebSocket connection closed: Session ID {}, User ID {} - Status: {} (Remaining sessions: {}, User Map Size: {})",
                 session.getId(), userId, status, sessions.size(), userSessionMap.size());
     }

     @Override
     public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
         Long userId = (Long) session.getAttributes().get("userId");
         // === Log Stack Trace ===
         logger.error("WebSocket transport error for Session ID {}, User ID {}: {}",
                      session.getId(), userId, exception.getMessage(), exception); // Add exception for stack trace
         // =======================
         sessions.remove(session);
         if (userId != null) {
             userSessionMap.remove(userId);
         }
         if (session.isOpen()) {
             try {
                session.close(CloseStatus.SERVER_ERROR);
             } catch (IOException e) {
                 logger.error("Error closing session {} after transport error: {}", session.getId(), e.getMessage());
             }
         }
     }
 }
package com.utegiftshop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utegiftshop.entity.ChatMessage;
import com.utegiftshop.repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class VendorChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(VendorChatWebSocketHandler.class);
    private final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Long> lastActivityMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    public VendorChatWebSocketHandler() {
        scheduler.scheduleAtFixedRate(this::sendHeartbeat, 30, 30, TimeUnit.SECONDS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        lastActivityMap.put(session.getId(), System.currentTimeMillis());
        logger.info("WebSocket connection established: {} (Total sessions: {})", session.getId(), sessions.size());
        
        sendJsonMessage(session, Map.of(
            "type", "SYSTEM",
            "content", "Kết nối thành công!",
            "timestamp", System.currentTimeMillis()
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        lastActivityMap.put(session.getId(), System.currentTimeMillis());
        logger.info("Received message from {}: {}", session.getId(), payload);

        try {
            Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);
            String type = (String) messageData.getOrDefault("type", "MESSAGE");
            
            if ("PING".equals(type)) {
                sendJsonMessage(session, Map.of("type", "PONG", "timestamp", System.currentTimeMillis()));
                return;
            }
            
            if ("MESSAGE".equals(type)) {
                // Lưu tin nhắn vào database
                String content = (String) messageData.get("content");
                Long senderId = messageData.get("senderId") != null ? 
                    Long.parseLong(messageData.get("senderId").toString()) : null;
                Long receiverId = messageData.get("receiverId") != null ? 
                    Long.parseLong(messageData.get("receiverId").toString()) : null;
                String senderRole = (String) messageData.getOrDefault("senderRole", "VENDOR");
                
                if (senderId != null && receiverId != null) {
                    String conversationId = generateConversationId(senderId, receiverId, senderRole);
                    
                    ChatMessage chatMessage = new ChatMessage(
                        senderId, receiverId, senderRole, content, conversationId
                    );
                    chatMessageRepository.save(chatMessage);
                    
                    messageData.put("conversationId", conversationId);
                    messageData.put("messageId", chatMessage.getId());
                }
            }
            
            broadcast(session, messageData);
            
        } catch (Exception e) {
            logger.error("Error processing message: {}", e.getMessage());
            broadcast(session, Map.of(
                "type", "MESSAGE",
                "content", payload,
                "sender", getSenderName(session)
            ));
        }
    }
    
    private String generateConversationId(Long senderId, Long receiverId, String senderRole) {
        if ("VENDOR".equals(senderRole)) {
            return "vendor_" + senderId + "_customer_" + receiverId;
        } else {
            return "vendor_" + receiverId + "_customer_" + senderId;
        }
    }

    private void broadcast(WebSocketSession sender, Map<String, Object> messageData) {
        if (!messageData.containsKey("sender")) {
            messageData.put("sender", getSenderName(sender));
        }
        if (!messageData.containsKey("timestamp")) {
            messageData.put("timestamp", System.currentTimeMillis());
        }
        
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    sendJsonMessage(session, messageData);
                } catch (Exception e) {
                    logger.error("Error broadcasting to session {}: {}", session.getId(), e.getMessage());
                }
            }
        }
    }

    private void sendJsonMessage(WebSocketSession session, Map<String, Object> data) throws IOException {
        if (session.isOpen()) {
            String jsonMessage = objectMapper.writeValueAsString(data);
            session.sendMessage(new TextMessage(jsonMessage));
        }
    }

    private void sendHeartbeat() {
        long now = System.currentTimeMillis();
        sessions.removeIf(session -> {
            try {
                if (!session.isOpen()) {
                    logger.info("Removing closed session: {}", session.getId());
                    return true;
                }
                
                Long lastActivity = lastActivityMap.get(session.getId());
                if (lastActivity != null && (now - lastActivity) > 300000) {
                    logger.warn("Session {} is stale, closing", session.getId());
                    session.close(CloseStatus.GOING_AWAY);
                    return true;
                }
                
                sendJsonMessage(session, Map.of("type", "PING", "timestamp", now));
                return false;
            } catch (Exception e) {
                logger.error("Error in heartbeat for session {}: {}", session.getId(), e.getMessage());
                return true;
            }
        });
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        lastActivityMap.put(session.getId(), System.currentTimeMillis());
        logger.debug("Received pong from session: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        lastActivityMap.remove(session.getId());
        logger.info("WebSocket connection closed: {} - Status: {} (Remaining: {})", 
            session.getId(), status, sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session);
        lastActivityMap.remove(session.getId());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private String getSenderName(WebSocketSession session) {
        if (session.getPrincipal() != null) {
            return session.getPrincipal().getName();
        }
        return "Vendor-" + session.getId().substring(0, 8);
    }
    
    @PreDestroy
    public void destroy() {
        scheduler.shutdown();
        sessions.forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.close(CloseStatus.GOING_AWAY);
                }
            } catch (Exception e) {
                logger.error("Error closing session on destroy: {}", e.getMessage());
            }
        });
    }
}
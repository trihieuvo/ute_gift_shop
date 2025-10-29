package com.utegiftshop.controller;

import com.utegiftshop.entity.ChatMessage;
import com.utegiftshop.repository.ChatMessageRepository;
import com.utegiftshop.repository.UserRepository;
import com.utegiftshop.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private UserRepository userRepository;

    // Lấy danh sách conversations của vendor
    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            
            User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Lấy tất cả tin nhắn liên quan đến user
            List<ChatMessage> allMessages = chatMessageRepository.findBySenderIdOrReceiverId(
                currentUser.getId(), currentUser.getId()
            );
            
            // Nhóm theo conversationId
            Map<String, List<ChatMessage>> groupedMessages = allMessages.stream()
                .collect(Collectors.groupingBy(ChatMessage::getConversationId));
            
            List<Map<String, Object>> conversations = new ArrayList<>();
            
            for (Map.Entry<String, List<ChatMessage>> entry : groupedMessages.entrySet()) {
                String conversationId = entry.getKey();
                List<ChatMessage> messages = entry.getValue();
                
                // Sắp xếp để lấy tin nhắn mới nhất
                messages.sort((m1, m2) -> {
                    LocalDateTime t1 = m1.getCreatedAt() != null ? m1.getCreatedAt() : m1.getTimestamp();
                    LocalDateTime t2 = m2.getCreatedAt() != null ? m2.getCreatedAt() : m2.getTimestamp();
                    return t2.compareTo(t1);
                });
                
                ChatMessage lastMessage = messages.get(0);
                
                // Xác định đối phương (partner)
                Long partnerId = lastMessage.getSenderId().equals(currentUser.getId()) 
                    ? lastMessage.getReceiverId() 
                    : lastMessage.getSenderId();
                
                Optional<User> partnerOpt = userRepository.findById(partnerId);
                if (!partnerOpt.isPresent()) continue;
                
                User partner = partnerOpt.get();
                
                // Đếm tin nhắn chưa đọc trong conversation này
                long unreadCount = messages.stream()
                    .filter(m -> m.getReceiverId().equals(currentUser.getId()) 
                              && !m.getIsRead())
                    .count();
                
                Map<String, Object> conversation = new HashMap<>();
                conversation.put("conversationId", conversationId);
                conversation.put("partnerId", partnerId);
                
                // Xử lý tên partner - chỉ dùng fullName
                String partnerName = partner.getFullName() != null ? partner.getFullName() : "User #" + partnerId;
                conversation.put("partnerName", partnerName);
                conversation.put("partnerEmail", partner.getEmail());
                conversation.put("lastMessage", lastMessage.getContent());
                
                LocalDateTime lastTime = lastMessage.getCreatedAt() != null 
                    ? lastMessage.getCreatedAt() 
                    : lastMessage.getTimestamp();
                conversation.put("lastMessageTime", lastTime);
                conversation.put("unreadCount", unreadCount);
                
                conversations.add(conversation);
            }
            
            // Sắp xếp theo thời gian tin nhắn mới nhất
            conversations.sort((c1, c2) -> {
                LocalDateTime t1 = (LocalDateTime) c1.get("lastMessageTime");
                LocalDateTime t2 = (LocalDateTime) c2.get("lastMessageTime");
                return t2.compareTo(t1);
            });
            
            return ResponseEntity.ok(conversations);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi tải danh sách: " + e.getMessage()));
        }
    }

    // Lấy lịch sử tin nhắn của 1 conversation
    @GetMapping("/messages/{conversationId}")
    public ResponseEntity<?> getMessages(
            @PathVariable String conversationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            
            List<ChatMessage> messages = chatMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);
            
            // Chuyển đổi sang DTO đơn giản
            List<Map<String, Object>> messageList = messages.stream()
                .map(msg -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", msg.getId());
                    map.put("senderId", msg.getSenderId());
                    map.put("receiverId", msg.getReceiverId());
                    map.put("content", msg.getContent());
                    map.put("timestamp", msg.getCreatedAt() != null ? msg.getCreatedAt() : msg.getTimestamp());
                    map.put("isRead", msg.getIsRead());
                    return map;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(messageList);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi tải tin nhắn: " + e.getMessage()));
        }
    }

    // Đánh dấu tin nhắn đã đọc
    @PutMapping("/messages/read")
    public ResponseEntity<?> markAsRead(
            @RequestParam Long receiverId, 
            @RequestParam String conversationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            
            User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Kiểm tra quyền
            if (!currentUser.getId().equals(receiverId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }
            
            List<ChatMessage> messages = chatMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);
            
            messages.stream()
                .filter(m -> m.getReceiverId().equals(receiverId) && !m.getIsRead())
                .forEach(m -> {
                    m.setIsRead(true);
                    chatMessageRepository.save(m);
                });
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã đánh dấu đọc"));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi đánh dấu đọc: " + e.getMessage()));
        }
    }
}
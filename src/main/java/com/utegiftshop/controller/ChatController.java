package com.utegiftshop.controller;

 import java.time.LocalDateTime;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Optional;
 import java.util.Set;
 import java.util.stream.Collectors;

 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.http.HttpStatus;
 import org.springframework.http.ResponseEntity;
 import org.springframework.security.core.Authentication;
 import org.springframework.security.core.annotation.AuthenticationPrincipal;
 import org.springframework.security.core.context.SecurityContextHolder;
 import org.springframework.security.core.userdetails.UserDetails;
 import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.utegiftshop.entity.ChatMessage;
import com.utegiftshop.entity.User;
import com.utegiftshop.repository.ChatMessageRepository;
import com.utegiftshop.repository.UserRepository;
import com.utegiftshop.security.service.UserDetailsImpl;

 @RestController
 @RequestMapping("/api/chat")
 public class ChatController {

     private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

     @Autowired
     private ChatMessageRepository chatMessageRepository;

     @Autowired
     private UserRepository userRepository;

     private UserDetailsImpl getCurrentUserDetails() {
         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
         if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
             return (UserDetailsImpl) authentication.getPrincipal();
         }
         return null;
     }

     @GetMapping("/conversations")
     public ResponseEntity<?> getConversations() {
         UserDetailsImpl userDetails = getCurrentUserDetails();
         if (userDetails == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
         }
         Long currentUserId = userDetails.getId();
         logger.info("Fetching conversations for User ID: {}", currentUserId);

         try {
             List<ChatMessage> allMessages = chatMessageRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(currentUserId);

             Map<String, ChatMessage> latestMessagesMap = allMessages.stream()
                 .collect(Collectors.toMap(
                     ChatMessage::getConversationId,
                     msg -> msg,
                     (existing, replacement) -> existing
                 ));

             if (latestMessagesMap.isEmpty()){
                  return ResponseEntity.ok(Collections.emptyList());
             }

             Set<Long> partnerIds = latestMessagesMap.values().stream()
                 .map(msg -> msg.getSenderId().equals(currentUserId) ? msg.getReceiverId() : msg.getSenderId())
                 .collect(Collectors.toSet());

             Map<Long, User> partnerUserMap = userRepository.findAllById(partnerIds).stream()
                 .collect(Collectors.toMap(User::getId, user -> user));

             List<Map<String, Object>> conversations = new ArrayList<>();
             for (ChatMessage lastMessage : latestMessagesMap.values()) {
                 Long partnerId = lastMessage.getSenderId().equals(currentUserId)
                     ? lastMessage.getReceiverId()
                     : lastMessage.getSenderId();

                 User partner = partnerUserMap.get(partnerId);
                 if (partner == null) {
                     continue;
                 }

                 long unreadCount = chatMessageRepository.countByConversationIdAndReceiverIdAndIsReadFalse(
                     lastMessage.getConversationId(), currentUserId
                 );

                 Map<String, Object> conversation = new HashMap<>();
                 conversation.put("conversationId", lastMessage.getConversationId());
                 conversation.put("partnerId", partnerId);
                 conversation.put("partnerName", partner.getFullName() != null ? partner.getFullName() : "User #" + partnerId);
                 conversation.put("partnerEmail", partner.getEmail());
                 conversation.put("lastMessage", lastMessage.getContent());
                 conversation.put("lastMessageTime", lastMessage.getCreatedAt());
                 conversation.put("unreadCount", unreadCount);

                 conversations.add(conversation);
             }

             conversations.sort((c1, c2) -> {
                 LocalDateTime t1 = (LocalDateTime) c1.get("lastMessageTime");
                 LocalDateTime t2 = (LocalDateTime) c2.get("lastMessageTime");
                 if (t1 == null && t2 == null) return 0;
                 if (t1 == null) return 1;
                 if (t2 == null) return -1;
                 return t2.compareTo(t1);
             });

             return ResponseEntity.ok(conversations);

         } catch (Exception e) {
             logger.error("Error fetching conversations for User ID {}: {}", currentUserId, e.getMessage(), e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Lỗi server khi tải danh sách cuộc trò chuyện."));
         }
     }

     @GetMapping("/messages/{conversationId}")
     public ResponseEntity<?> getMessages(@PathVariable String conversationId) {
         UserDetailsImpl userDetails = getCurrentUserDetails();
         if (userDetails == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
         }
          Long currentUserId = userDetails.getId();
          logger.info("Fetching messages for conversation: {}, User ID: {}", conversationId, currentUserId);

         boolean belongsToConversation = false;
         String[] parts = conversationId.split("_");
         try {
             if (conversationId.startsWith("conv_") && parts.length == 3) {
                 long id1 = Long.parseLong(parts[1]);
                 long id2 = Long.parseLong(parts[2]);
                 belongsToConversation = (currentUserId == id1 || currentUserId == id2);
             }
         } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
              logger.error("Invalid conversationId format: {}", conversationId, e);
         }

         if (!belongsToConversation) {
              logger.warn("User {} attempted to access conversation {} without permission.", currentUserId, conversationId);
              return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Forbidden"));
         }


         try {
             List<ChatMessage> messages = chatMessageRepository
                 .findByConversationIdOrderByCreatedAtAsc(conversationId);

             List<Map<String, Object>> messageList = messages.stream()
                 .map(msg -> {
                     Map<String, Object> map = new HashMap<>();
                     map.put("id", msg.getId());
                     map.put("senderId", msg.getSenderId());
                     map.put("receiverId", msg.getReceiverId());
                     map.put("content", msg.getContent());
                     map.put("timestamp", msg.getCreatedAt());
                     map.put("isRead", msg.getIsRead());
                     map.put("senderRole", msg.getSenderRole());
                     return map;
                 })
                 .collect(Collectors.toList());

             return ResponseEntity.ok(messageList);

         } catch (Exception e) {
             logger.error("Error fetching messages for conversation {}: {}", conversationId, e.getMessage(), e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Lỗi server khi tải tin nhắn."));
         }
     }

     @PutMapping("/messages/read")
     @Transactional
     public ResponseEntity<?> markAsRead(
             @RequestParam String conversationId,
             @AuthenticationPrincipal UserDetails userDetails
             ) {

          if (userDetails == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
          }
          Long currentUserId;
          if (userDetails instanceof UserDetailsImpl) {
               currentUserId = ((UserDetailsImpl) userDetails).getId();
          } else {
               Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
               if(userOpt.isEmpty()){
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Không tìm thấy thông tin user hiện tại."));
               }
               currentUserId = userOpt.get().getId();
          }

          logger.info("Marking messages as read for conversation: {}, Receiver ID: {}", conversationId, currentUserId);

         try {
             List<ChatMessage> unreadMessages = chatMessageRepository
                 .findByConversationIdAndReceiverIdAndIsReadFalse(conversationId, currentUserId);

             if (unreadMessages.isEmpty()) {
                  return ResponseEntity.ok(Map.of("success", true, "message", "Không có tin nhắn mới."));
             }

             for (ChatMessage msg : unreadMessages) {
                 msg.setIsRead(true);
             }
             chatMessageRepository.saveAll(unreadMessages);

             return ResponseEntity.ok(Map.of("success", true, "message", "Đã đánh dấu " + unreadMessages.size() + " tin nhắn là đã đọc."));

         } catch (Exception e) {
             logger.error("Error marking messages as read for conversation {}: {}", conversationId, e.getMessage(), e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Lỗi server khi đánh dấu đã đọc."));
         }
     }
 }
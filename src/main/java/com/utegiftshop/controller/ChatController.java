package com.utegiftshop.controller;

 import com.utegiftshop.entity.ChatMessage;
 import com.utegiftshop.repository.ChatMessageRepository;
 import com.utegiftshop.repository.UserRepository;
 import com.utegiftshop.security.service.UserDetailsImpl;
 import com.utegiftshop.entity.User;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.http.HttpStatus;
 import org.springframework.http.ResponseEntity;
 import org.springframework.security.core.Authentication;
 import org.springframework.security.core.context.SecurityContextHolder;
 import org.springframework.transaction.annotation.Transactional;
 import org.springframework.web.bind.annotation.*;
 import org.springframework.security.core.annotation.AuthenticationPrincipal; // ✅ THÊM IMPORT NÀY
 import org.springframework.security.core.userdetails.UserDetails; // ✅ THÊM IMPORT NÀY

 import java.time.LocalDateTime;
 import java.util.*;
 import java.util.stream.Collectors;

 @RestController
 @RequestMapping("/api/chat")
 public class ChatController {

     private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

     @Autowired
     private ChatMessageRepository chatMessageRepository;

     @Autowired
     private UserRepository userRepository;

     // Helper lấy UserDetailsImpl hiện tại
     private UserDetailsImpl getCurrentUserDetails() {
         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
         if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
             return (UserDetailsImpl) authentication.getPrincipal();
         }
         return null;
     }

     // Lấy danh sách conversations (Đã tối ưu)
     @GetMapping("/conversations")
     public ResponseEntity<?> getConversations() {
         UserDetailsImpl userDetails = getCurrentUserDetails();
         if (userDetails == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
         }
         Long currentUserId = userDetails.getId();
         logger.info("Fetching conversations for User ID: {}", currentUserId);

         try {
             // Lấy tất cả tin nhắn liên quan, sắp xếp mới nhất trước
             List<ChatMessage> allMessages = chatMessageRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(currentUserId);

             // Nhóm theo conversationId và chỉ giữ lại tin nhắn mới nhất của mỗi conversation
             Map<String, ChatMessage> latestMessagesMap = allMessages.stream()
                 .collect(Collectors.toMap(
                     ChatMessage::getConversationId, // Key là conversationId
                     msg -> msg,                     // Value là chính tin nhắn đó
                     (existing, replacement) -> existing // Nếu trùng key, giữ lại cái đầu tiên (là cái mới nhất do đã sort DESC)
                 ));

             if (latestMessagesMap.isEmpty()){
                  logger.info("No conversations found for User ID: {}", currentUserId);
                  return ResponseEntity.ok(Collections.emptyList()); // Trả về list rỗng
             }

             List<Map<String, Object>> conversations = new ArrayList<>();
             // Lấy danh sách ID của các partner để query User một lần
             Set<Long> partnerIds = latestMessagesMap.values().stream()
                 .map(msg -> msg.getSenderId().equals(currentUserId) ? msg.getReceiverId() : msg.getSenderId())
                 .collect(Collectors.toSet());

             // Query thông tin các partner
             Map<Long, User> partnerUserMap = userRepository.findAllById(partnerIds).stream()
                 .collect(Collectors.toMap(User::getId, user -> user));

             // Tạo response DTO
             for (ChatMessage lastMessage : latestMessagesMap.values()) {
                 Long partnerId = lastMessage.getSenderId().equals(currentUserId)
                     ? lastMessage.getReceiverId()
                     : lastMessage.getSenderId();

                 User partner = partnerUserMap.get(partnerId);
                 if (partner == null) {
                     logger.warn("Partner user not found for ID: {} in conversation {}", partnerId, lastMessage.getConversationId());
                     continue; // Bỏ qua nếu không tìm thấy partner
                 }

                 // Đếm tin nhắn chưa đọc (Query trực tiếp sẽ hiệu quả hơn)
                 long unreadCount = chatMessageRepository.countByConversationIdAndReceiverIdAndIsReadFalse(
                     lastMessage.getConversationId(), currentUserId
                 );

                 Map<String, Object> conversation = new HashMap<>();
                 conversation.put("conversationId", lastMessage.getConversationId());
                 conversation.put("partnerId", partnerId);
                 conversation.put("partnerName", partner.getFullName() != null ? partner.getFullName() : "User #" + partnerId);
                 conversation.put("partnerEmail", partner.getEmail()); // Có thể cần hoặc không
                 conversation.put("lastMessage", lastMessage.getContent());
                 conversation.put("lastMessageTime", lastMessage.getCreatedAt()); // Dùng createdAt
                 conversation.put("unreadCount", unreadCount);

                 conversations.add(conversation);
             }

             // Sắp xếp lại theo thời gian giảm dần (vì Map không giữ thứ tự)
             conversations.sort((c1, c2) -> {
                 LocalDateTime t1 = (LocalDateTime) c1.get("lastMessageTime");
                 LocalDateTime t2 = (LocalDateTime) c2.get("lastMessageTime");
                 if (t1 == null && t2 == null) return 0;
                 if (t1 == null) return 1;
                 if (t2 == null) return -1;
                 return t2.compareTo(t1);
             });

              logger.info("Successfully fetched {} conversations for User ID: {}", conversations.size(), currentUserId);
             return ResponseEntity.ok(conversations);

         } catch (Exception e) {
             logger.error("Error fetching conversations for User ID {}: {}", currentUserId, e.getMessage(), e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Lỗi server khi tải danh sách cuộc trò chuyện."));
         }
     }

     // Lấy lịch sử tin nhắn của 1 conversation (Giữ nguyên logic nhưng trả về LocalDateTime)
     @GetMapping("/messages/{conversationId}")
     public ResponseEntity<?> getMessages(@PathVariable String conversationId) {
         UserDetailsImpl userDetails = getCurrentUserDetails();
         if (userDetails == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
         }
          Long currentUserId = userDetails.getId();
          logger.info("Fetching messages for conversation: {}, User ID: {}", conversationId, currentUserId);


         // Kiểm tra xem user có thuộc conversation này không (bảo mật)
         // Giả sử conversationId có dạng "conv_ID_NhoHon_ID_LonHon" hoặc "vendor_VID_customer_CID"
         // Cần đảm bảo logic generateConversationId nhất quán
         boolean belongsToConversation = false;
         String[] parts = conversationId.split("_");
         try {
             if (conversationId.startsWith("conv_") && parts.length == 3) {
                 long id1 = Long.parseLong(parts[1]);
                 long id2 = Long.parseLong(parts[2]);
                 belongsToConversation = (currentUserId == id1 || currentUserId == id2);
             } else if (conversationId.startsWith("vendor_") && parts.length == 4) {
                 long vendorId = Long.parseLong(parts[1]);
                 long customerId = Long.parseLong(parts[3]);
                 belongsToConversation = (currentUserId == vendorId || currentUserId == customerId);
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

             // Chuyển đổi sang DTO đơn giản
             List<Map<String, Object>> messageList = messages.stream()
                 .map(msg -> {
                     Map<String, Object> map = new HashMap<>();
                     map.put("id", msg.getId());
                     map.put("senderId", msg.getSenderId());
                     map.put("receiverId", msg.getReceiverId());
                     map.put("content", msg.getContent());
                     map.put("timestamp", msg.getCreatedAt()); // Trả về LocalDateTime
                     map.put("isRead", msg.getIsRead());
                     map.put("senderRole", msg.getSenderRole()); // Thêm role
                     // Lấy tên người gửi (Tùy chọn, có thể làm ở frontend)
                     // Optional<User> sender = userRepository.findById(msg.getSenderId());
                     // map.put("senderName", sender.map(User::getFullName).orElse("User " + msg.getSenderId()));
                     return map;
                 })
                 .collect(Collectors.toList());

              logger.info("Found {} messages for conversation: {}", messageList.size(), conversationId);
             return ResponseEntity.ok(messageList);

         } catch (Exception e) {
             logger.error("Error fetching messages for conversation {}: {}", conversationId, e.getMessage(), e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Lỗi server khi tải tin nhắn."));
         }
     }

     // Đánh dấu tin nhắn đã đọc (Sử dụng Transactional)
     @PutMapping("/messages/read")
     @Transactional // Quan trọng: Đảm bảo các thay đổi được commit
     public ResponseEntity<?> markAsRead(
             @RequestParam String conversationId,
             @AuthenticationPrincipal UserDetails userDetails // Sử dụng UserDetails thay vì UserDetailsImpl trực tiếp
             ) {

          if (userDetails == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
          }
          // Lấy ID người dùng hiện tại (người nhận) từ UserDetails
          // Cần ép kiểu hoặc tìm user trong DB nếu UserDetails mặc định không có ID
          Long currentUserId;
          if (userDetails instanceof UserDetailsImpl) {
               currentUserId = ((UserDetailsImpl) userDetails).getId();
          } else {
               // Fallback: Tìm user bằng username (email) trong DB
               Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
               if(userOpt.isEmpty()){
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Không tìm thấy thông tin user hiện tại."));
               }
               currentUserId = userOpt.get().getId();
          }


          logger.info("Marking messages as read for conversation: {}, Receiver ID: {}", conversationId, currentUserId);

         try {
              // Lấy danh sách tin nhắn chưa đọc trong conversation này mà user hiện tại là người nhận
             List<ChatMessage> unreadMessages = chatMessageRepository
                 .findByConversationIdAndReceiverIdAndIsReadFalse(conversationId, currentUserId);

             if (unreadMessages.isEmpty()) {
                 logger.info("No unread messages found for conversation {} and receiver {}", conversationId, currentUserId);
                  return ResponseEntity.ok(Map.of("success", true, "message", "Không có tin nhắn mới."));
             }

             // Đánh dấu đã đọc và lưu
             for (ChatMessage msg : unreadMessages) {
                 msg.setIsRead(true);
             }
             chatMessageRepository.saveAll(unreadMessages); // Lưu tất cả thay đổi trong 1 transaction

              logger.info("Marked {} messages as read for conversation {} and receiver {}", unreadMessages.size(), conversationId, currentUserId);
             return ResponseEntity.ok(Map.of("success", true, "message", "Đã đánh dấu " + unreadMessages.size() + " tin nhắn là đã đọc."));

         } catch (Exception e) {
             logger.error("Error marking messages as read for conversation {}: {}", conversationId, e.getMessage(), e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Lỗi server khi đánh dấu đã đọc."));
         }
     }
 }
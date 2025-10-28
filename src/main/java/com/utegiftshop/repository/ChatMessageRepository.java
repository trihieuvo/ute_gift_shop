package com.utegiftshop.repository;

import com.utegiftshop.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * Tìm tất cả tin nhắn theo conversationId, sắp xếp theo createdAt
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationId = :conversationId ORDER BY " +
           "CASE WHEN cm.createdAt IS NOT NULL THEN cm.createdAt ELSE cm.timestamp END ASC")
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(@Param("conversationId") String conversationId);
    
    /**
     * Tìm tất cả tin nhắn liên quan đến một user (là sender hoặc receiver)
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.senderId = :userId OR cm.receiverId = :userId")
    List<ChatMessage> findBySenderIdOrReceiverId(
        @Param("userId") Long senderId, 
        @Param("userId") Long receiverId
    );
    
    /**
     * Đếm số tin nhắn chưa đọc của một người nhận
     */
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.receiverId = :receiverId AND cm.isRead = false")
    long countByReceiverIdAndIsReadFalse(@Param("receiverId") Long receiverId);
    
    /**
     * Tìm tin nhắn cuối cùng của một cuộc trò chuyện
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationId = :conversationId ORDER BY " +
           "CASE WHEN cm.createdAt IS NOT NULL THEN cm.createdAt ELSE cm.timestamp END DESC")
    ChatMessage findLastMessageByConversationId(@Param("conversationId") String conversationId);
    
    /**
     * Tìm tất cả conversationIds liên quan đến một user
     */
    @Query("SELECT DISTINCT cm.conversationId FROM ChatMessage cm WHERE cm.senderId = :userId OR cm.receiverId = :userId")
    List<String> findConversationsByUserId(@Param("userId") Long userId);
}
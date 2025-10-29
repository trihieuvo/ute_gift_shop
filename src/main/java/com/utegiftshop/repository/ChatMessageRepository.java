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
     * Tìm tất cả tin nhắn theo conversationId, sắp xếp theo createdAt TĂNG DẦN (cũ nhất trước)
     */
    // Sửa ORDER BY chỉ dùng createdAt
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationId = :conversationId ORDER BY cm.createdAt ASC")
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(@Param("conversationId") String conversationId);

    /**
     * Tìm tất cả tin nhắn liên quan đến một user (là sender hoặc receiver), sắp xếp theo createdAt GIẢM DẦN (mới nhất trước)
     * Dùng để lấy danh sách conversations
     */
    // Sửa ORDER BY chỉ dùng createdAt
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.senderId = :userId OR cm.receiverId = :userId ORDER BY cm.createdAt DESC")
    List<ChatMessage> findBySenderIdOrReceiverIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * Đếm số tin nhắn chưa đọc của một người nhận TRONG MỘT conversation cụ thể
     */
    long countByConversationIdAndReceiverIdAndIsReadFalse(String conversationId, Long receiverId);

    /**
     * Lấy các tin nhắn chưa đọc của một người nhận TRONG MỘT conversation cụ thể
     */
     List<ChatMessage> findByConversationIdAndReceiverIdAndIsReadFalse(String conversationId, Long receiverId);

    // Không cần hàm findLastMessageByConversationId nữa vì hàm findBySenderIdOrReceiverIdOrderByCreatedAtDesc đã sắp xếp mới nhất lên đầu
}
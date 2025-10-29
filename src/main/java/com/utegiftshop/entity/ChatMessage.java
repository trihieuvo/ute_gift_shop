package com.utegiftshop.entity;

import jakarta.persistence.*; // Import đúng
import java.time.LocalDateTime; // Sử dụng LocalDateTime

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "sender_role", nullable = false)
    private String senderRole; // VENDOR hoặc CUSTOMER

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "conversation_id", nullable = false)
    private String conversationId;

    // Bỏ timestamp cũ, chỉ dùng createdAt
    // @Column(name = "timestamp")
    // private LocalDateTime timestamp;

    @Column(name = "created_at", nullable = false, updatable = false) // Đảm bảo không null và không update
    private LocalDateTime createdAt;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false; // Mặc định là false

    // Constructors
    public ChatMessage() {
        // Không cần set createdAt ở đây nữa nếu dùng @PrePersist
    }

     // Constructor với các tham số cần thiết
     public ChatMessage(Long senderId, Long receiverId, String senderRole, String content, String conversationId) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderRole = senderRole;
        this.content = content;
        this.conversationId = conversationId;
        // Không cần set createdAt ở đây nữa
        this.isRead = false; // Mặc định chưa đọc
    }


    // Tự động set createdAt trước khi lưu lần đầu
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isRead == null) { // Đảm bảo isRead không bao giờ null
            this.isRead = false;
        }
    }

    // --- Getters and Setters ---
    // (Lombok @Getter/@Setter sẽ tự tạo hoặc bạn tự viết)

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

}
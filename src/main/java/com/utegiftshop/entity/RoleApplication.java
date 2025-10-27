package com.utegiftshop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.sql.Timestamp;

@Entity
@Table(name = "role_applications")
@Getter
@Setter
@NoArgsConstructor
public class RoleApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "requested_role", length = 50, nullable = false)
    private String requestedRole; // "Vendor" hoặc "Shipper"

    @Column(name = "status", length = 50, nullable = false)
    private String status; // "PENDING", "APPROVED", "REJECTED"

    @Column(columnDefinition = "TEXT")
    private String message; // Lời nhắn/lý do của người dùng

    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    @Column(name = "updated_at")
    private Timestamp updatedAt = new Timestamp(System.currentTimeMillis());
}
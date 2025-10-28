package com.utegiftshop.entity;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Đảm bảo chỉ sản phẩm đã mua mới được đánh giá
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_detail_id", nullable = false, unique = true)
    private OrderDetail orderDetail;

    @Column(nullable = false)
    private Short rating; // Số sao (1-5)

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());
    
    @Column(name = "vendor_reply", columnDefinition = "TEXT", nullable = true)
    private String vendorReply; // Nội dung phản hồi của vendor
    @Column(name = "replied_at", nullable = true)
    private Timestamp repliedAt;
}
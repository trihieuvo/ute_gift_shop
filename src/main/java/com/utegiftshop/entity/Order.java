package com.utegiftshop.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List; 

import com.fasterxml.jackson.annotation.JsonManagedReference; // BỔ SUNG IMPORT
import jakarta.persistence.*; 
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Người đặt hàng

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(length = 50, nullable = false)
    private String status; // NEW, CONFIRMED, DELIVERING, DELIVERED, CANCELLED

    @Column(name = "shipping_address", nullable = false, columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "payment_method", length = 50, nullable = false)
    private String paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id")
    private User shipper; // Người giao hàng

    @Column(name = "order_date", updatable = false)
    private Timestamp orderDate = new Timestamp(System.currentTimeMillis());

    // === BỔ SUNG ANNOTATION NÀY ===
    @JsonManagedReference // Đánh dấu đây là "cha", nó sẽ được serialize
    @OneToMany(mappedBy = "order", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<OrderDetail> orderDetails;
}
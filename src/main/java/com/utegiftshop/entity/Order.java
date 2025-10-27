package com.utegiftshop.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Người đặt hàng

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(length = 50, nullable = false)
    private String status;

    @Column(name = "shipping_address", nullable = false, columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "payment_method", length = 50, nullable = false)
    private String paymentMethod;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shipper_id")
    private User shipper; // Người giao hàng

    @Column(name = "order_date", updatable = false)
    private Timestamp orderDate = new Timestamp(System.currentTimeMillis());

    @Column(name = "delivery_note", columnDefinition = "TEXT")
    private String deliveryNote;

    @Column(name = "proof_of_delivery_image_url", columnDefinition = "TEXT")
    private String proofOfDeliveryImageUrl;

    // === CHỈNH SỬA Ở ĐÂY ===
    @Column(name = "is_cod_reconciled", nullable = false, columnDefinition = "boolean default false")
    private boolean isCodReconciled = false; // Đã đối soát tiền COD hay chưa
    // === KẾT THÚC CHỈNH SỬA ===

    @JsonManagedReference
    @OneToMany(mappedBy = "order", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<OrderDetail> orderDetails;
}
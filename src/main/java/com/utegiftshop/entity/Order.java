package com.utegiftshop.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
    
    @Column(name = "payment_code", length = 50, unique = true)
    private String paymentCode; // Dùng để dò giao dịch trên SePay

    // === BỔ SUNG CÁC TRƯỜNG CHO CỔNG THANH TOÁN ===
    @Column(name = "payment_status", length = 20)
    private String paymentStatus; // PENDING, SUCCESS, FAILED

    @Column(name = "payment_trans_id", length = 50)
    private String paymentTransId; // Mã giao dịch của cổng thanh toán

    @Column(name = "payment_request_id", length = 50)
    private String paymentRequestId; // Mã requestId của đơn hàng

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shipper_id")
    private User shipper; // Người giao hàng

    @Column(name = "order_date", updatable = false)
    private Timestamp orderDate = new Timestamp(System.currentTimeMillis());

    @Column(name = "delivery_note", columnDefinition = "TEXT")
    private String deliveryNote;

    @Column(name = "proof_of_delivery_image_url", columnDefinition = "TEXT")
    private String proofOfDeliveryImageUrl;


    @Column(name = "is_cod_reconciled", nullable = false, columnDefinition = "boolean default false")
    private boolean isCodReconciled = false; // Đã đối soát tiền COD hay chưa
  

    @JsonManagedReference
    @OneToMany(mappedBy = "order", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<OrderDetail> orderDetails;
    


    
}
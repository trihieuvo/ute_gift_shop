package com.utegiftshop.dto.response;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import com.utegiftshop.entity.Order;
import com.utegiftshop.entity.User;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO chứa đầy đủ thông tin chi tiết đơn hàng cần thiết cho Shipper.
 */
@Getter
@Setter
public class ShipperOrderDetailDto {
    private Long id;
    private String status;
    private Timestamp orderDate;
    
    // Thông tin người nhận (lấy từ User đặt hàng)
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    
    // Thông tin thanh toán
    private String paymentMethod;
    private BigDecimal totalAmount;
    
    // Ghi chú giao hàng
    private String deliveryNote;
    
    // Chi tiết sản phẩm
    private List<ShipperProductDetailDto> products;

    public ShipperOrderDetailDto(Order order) {
        this.id = order.getId();
        this.status = order.getStatus();
        this.orderDate = order.getOrderDate();
        
        User customer = order.getUser(); // Assumes User is EAGER fetched
        if (customer != null) {
            this.recipientName = customer.getFullName();
            this.recipientPhone = customer.getPhoneNumber();
        } else {
            this.recipientName = "N/A";
            this.recipientPhone = "N/A";
        }
        
        this.shippingAddress = order.getShippingAddress();
        this.paymentMethod = order.getPaymentMethod();
        this.totalAmount = order.getTotalAmount();
        this.deliveryNote = order.getDeliveryNote();
        
        if (order.getOrderDetails() != null) {
            this.products = order.getOrderDetails().stream()
                                .map(ShipperProductDetailDto::new)
                                .collect(Collectors.toList());
        } else {
            this.products = Collections.emptyList();
        }
    }
}
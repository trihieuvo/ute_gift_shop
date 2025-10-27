package com.utegiftshop.dto.response;

import java.math.BigDecimal;
import java.sql.Timestamp;
import com.utegiftshop.entity.Order;
import com.utegiftshop.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShipperOrderDto {
    private Long id;
    private Timestamp orderDate;
    private String recipientName; 
    private String recipientPhone; 
    private String shippingAddress;
    private BigDecimal totalAmount;
    private String status;
    private String paymentMethod; 
    
    // === BỔ SUNG: HIỂN THỊ GHI CHÚ TRÊN DANH SÁCH LỊCH SỬ ===
    private String deliveryNote;
    // === KẾT THÚC BỔ SUNG ===

    public ShipperOrderDto(Order order) {
        this.id = order.getId();
        this.orderDate = order.getOrderDate();
        
        User customer = order.getUser();
        if (customer != null) {
            this.recipientName = customer.getFullName();
            this.recipientPhone = customer.getPhoneNumber();
        } else {
            this.recipientName = "N/A";
            this.recipientPhone = "N/A";
        }
        
        this.shippingAddress = order.getShippingAddress();
        this.totalAmount = order.getTotalAmount();
        this.status = order.getStatus();
        this.paymentMethod = order.getPaymentMethod();
        
        // === BỔ SUNG ===
        this.deliveryNote = order.getDeliveryNote();
        // === KẾT THÚC BỔ SUNG ===
    }
}
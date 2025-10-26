package com.utegiftshop.dto.response;

import java.math.BigDecimal;
import java.sql.Timestamp;
import com.utegiftshop.entity.Order;
import com.utegiftshop.entity.User; // BỔ SUNG
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

    public ShipperOrderDto(Order order) {
        this.id = order.getId();
        this.orderDate = order.getOrderDate();
        
        // === THAY ĐỔI: Lấy thông tin từ User liên kết ===
        User customer = order.getUser();
        if (customer != null) {
            this.recipientName = customer.getFullName();
            this.recipientPhone = customer.getPhoneNumber();
        } else {
            this.recipientName = "N/A";
            this.recipientPhone = "N/A";
        }
        // === KẾT THÚC THAY ĐỔI ===
        
        this.shippingAddress = order.getShippingAddress();
        this.totalAmount = order.getTotalAmount();
        this.status = order.getStatus();
        this.paymentMethod = order.getPaymentMethod();
    }
}
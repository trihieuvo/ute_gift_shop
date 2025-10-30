package com.utegiftshop.dto.response;

import java.math.BigDecimal;
import java.sql.Timestamp;

import com.utegiftshop.entity.Order;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminOrderDto {

    private Long id;
    private Timestamp orderDate;
    private BigDecimal totalAmount;
    private String status;
    private String paymentMethod;
    private UserInfo user;
    private ShipperInfo shipper;

    // Lớp nội bộ để chỉ lấy thông tin cần thiết của User
    @Getter
    @Setter
    private static class UserInfo {
        private String fullName;
        
        public UserInfo(com.utegiftshop.entity.User userEntity) {
            if (userEntity != null) {
                this.fullName = userEntity.getFullName();
            }
        }
    }
    
    // Lớp nội bộ để chỉ lấy thông tin cần thiết của Shipper
    @Getter
    @Setter
    private static class ShipperInfo {
        private Long id;
        private String fullName;

        public ShipperInfo(com.utegiftshop.entity.User shipperEntity) {
            if (shipperEntity != null) {
                this.id = shipperEntity.getId();
                this.fullName = shipperEntity.getFullName();
            }
        }
    }

    // Constructor chính để chuyển đổi từ Order entity sang DTO
    public AdminOrderDto(Order order) {
        this.id = order.getId();
        this.orderDate = order.getOrderDate();
        this.totalAmount = order.getTotalAmount();
        this.status = order.getStatus();
        this.paymentMethod = order.getPaymentMethod();
        
        if (order.getUser() != null) {
            this.user = new UserInfo(order.getUser());
        }

        if (order.getShipper() != null) {
            this.shipper = new ShipperInfo(order.getShipper());
        }
    }
}
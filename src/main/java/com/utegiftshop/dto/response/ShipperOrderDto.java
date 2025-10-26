package com.utegiftshop.dto.response;

import java.math.BigDecimal;
import java.sql.Timestamp;
import com.utegiftshop.entity.Order;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO chứa thông tin tóm tắt đơn hàng cho Shipper.
 * Chỉ bao gồm các trường cần thiết để hiển thị trên danh sách.
 */
@Getter
@Setter
public class ShipperOrderDto {
    private Long id;
    private Timestamp orderDate;
    private String recipientName; // Tên người nhận
    private String shippingAddress;
    private BigDecimal totalAmount;
    private String status;

    /**
     * Constructor để chuyển đổi từ Entity Order sang ShipperOrderDto.
     * @param order Entity Order lấy từ database (đã join với User).
     */
    public ShipperOrderDto(Order order) {
        this.id = order.getId();
        this.orderDate = order.getOrderDate();
        
        // Lấy tên người nhận hàng từ đối tượng User (người đặt hàng)
        if (order.getUser() != null) {
            this.recipientName = order.getUser().getFullName();
        } else {
            this.recipientName = "N/A";
        }
        
        this.shippingAddress = order.getShippingAddress();
        this.totalAmount = order.getTotalAmount();
        this.status = order.getStatus();
    }
}
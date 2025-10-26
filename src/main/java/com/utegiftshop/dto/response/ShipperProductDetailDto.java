package com.utegiftshop.dto.response;

import java.math.BigDecimal;
import com.utegiftshop.entity.OrderDetail;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO chứa thông tin chi tiết một sản phẩm trong đơn hàng cho Shipper.
 */
@Getter
@Setter
public class ShipperProductDetailDto {
    private String productName;
    private Integer quantity;
    private BigDecimal pricePerUnit; // Giá 1 sản phẩm tại thời điểm mua
    private BigDecimal totalPrice;   // Tổng giá cho sản phẩm này (price * quantity)

    public ShipperProductDetailDto(OrderDetail detail) {
        if (detail.getProduct() != null) {
            this.productName = detail.getProduct().getName();
        } else {
            this.productName = "Sản phẩm không xác định";
        }
        this.quantity = detail.getQuantity();
        this.pricePerUnit = detail.getPrice();
        if (this.pricePerUnit != null && this.quantity != null) {
            this.totalPrice = this.pricePerUnit.multiply(new BigDecimal(this.quantity));
        } else {
            this.totalPrice = BigDecimal.ZERO;
        }
    }
}
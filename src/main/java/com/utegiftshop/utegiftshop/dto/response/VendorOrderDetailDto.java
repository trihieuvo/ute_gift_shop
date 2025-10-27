package com.utegiftshop.dto.response;

import com.utegiftshop.entity.OrderDetail;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class VendorOrderDetailDto {
    private Long productId;
    private String productName;
    private String productImageUrl; // Lấy ảnh từ Product
    private Integer quantity;
    private BigDecimal price; // Giá tại thời điểm mua
    private BigDecimal totalItemPrice;

    public VendorOrderDetailDto(OrderDetail detail) {
        if (detail.getProduct() != null) {
            this.productId = detail.getProduct().getId();
            this.productName = detail.getProduct().getName();
            this.productImageUrl = detail.getProduct().getImageUrl(); // Lấy URL ảnh
        } else {
            this.productName = "Sản phẩm không xác định";
        }
        this.quantity = detail.getQuantity();
        this.price = detail.getPrice();
        if (this.price != null && this.quantity != null) {
            this.totalItemPrice = this.price.multiply(new BigDecimal(this.quantity));
        } else {
             this.totalItemPrice = BigDecimal.ZERO;
        }
    }
}
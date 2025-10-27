package com.utegiftshop.dto.response;

import java.math.BigDecimal;

import com.utegiftshop.entity.OrderDetail; // THÊM IMPORT
import com.utegiftshop.entity.Product;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VendorOrderDetailDto {
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalItemPrice;

    public VendorOrderDetailDto(OrderDetail detail) {
        if (detail.getProduct() != null) {
            Product product = detail.getProduct();
            this.productId = product.getId();
            this.productName = product.getName();
            
            // === THAY ĐỔI CÁCH LẤY ẢNH ===
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                this.productImageUrl = product.getImages().get(0).getImageUrl();
            }
            // === KẾT THÚC THAY ĐỔI ===

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
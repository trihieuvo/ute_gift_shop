package com.utegiftshop.dto.response;

import java.math.BigDecimal;

import com.utegiftshop.entity.Product; // THÊM IMPORT

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductVendorDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stockQuantity;
    private String imageUrl; // Giữ lại để làm ảnh đại diện
    private boolean isActive;
    private String categoryName;

    public ProductVendorDto(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.price = product.getPrice();
        this.stockQuantity = product.getStockQuantity();
        this.isActive = product.isActive();
        
        // === THAY ĐỔI: Lấy ảnh đầu tiên làm ảnh đại diện ===
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            this.imageUrl = product.getImages().get(0).getImageUrl();
        } else {
            this.imageUrl = null; // Hoặc một URL ảnh mặc định
        }
        // === KẾT THÚC THAY ĐỔI ===

        if (product.getCategory() != null) {
            this.categoryName = product.getCategory().getName();
        } else {
            this.categoryName = "N/A";
        }
    }
}
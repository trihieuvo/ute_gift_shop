// src/main/java/com/utegiftshop/dto/response/ProductVendorDto.java
package com.utegiftshop.dto.response;

import java.math.BigDecimal;
import com.utegiftshop.entity.Product;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductVendorDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stockQuantity;
    private String imageUrl; // Ảnh đại diện (sẽ có timestamp)
    private boolean isActive;
    private String categoryName;

    public ProductVendorDto(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.price = product.getPrice();
        this.stockQuantity = product.getStockQuantity();
        this.isActive = product.isActive(); // Đảm bảo đúng tên field 'active' từ Product entity

        String firstImageUrl = null;
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            // Lấy URL gốc từ ảnh đầu tiên
             firstImageUrl = product.getImages().get(0).getImageUrl();
             // *** XÓA BỎ LOGIC THÊM TIMESTAMP Ở ĐÂY ***
             // if (firstImageUrl != null && !firstImageUrl.isBlank()) {
             //     if (firstImageUrl.contains("?")) { ... } else { ... }
             // } else { ... }
             // *** KẾT THÚC XÓA BỎ ***
        }

        // Gán trực tiếp URL gốc (có thể là null)
        this.imageUrl = firstImageUrl; // <-- SỬA LẠI DÒNG NÀY

        this.categoryName = (product.getCategory() != null) ? product.getCategory().getName() : "N/A";
    }
}
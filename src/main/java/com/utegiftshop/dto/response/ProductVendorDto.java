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
        }

        // *** THÊM TIMESTAMP VÀO ĐÂY ***
        if (firstImageUrl != null && !firstImageUrl.isBlank()) {
            // Kiểm tra xem URL đã có dấu ? chưa để tránh lỗi ?t=...?t=...
            if (firstImageUrl.contains("?")) {
                 this.imageUrl = firstImageUrl + "&t=" + System.currentTimeMillis(); // Dùng & nếu đã có ?
            } else {
                 this.imageUrl = firstImageUrl + "?t=" + System.currentTimeMillis(); // Dùng ? nếu chưa có
            }
        } else {
            this.imageUrl = null; // Hoặc URL ảnh mặc định của bạn
        }
        // *** KẾT THÚC THÊM TIMESTAMP ***

        this.categoryName = (product.getCategory() != null) ? product.getCategory().getName() : "N/A";
    }
}
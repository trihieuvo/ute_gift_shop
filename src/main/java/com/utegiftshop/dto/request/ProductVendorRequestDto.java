package com.utegiftshop.dto.request;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductVendorRequestDto {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String imageUrl; // URL ảnh đại diện
    private Integer categoryId; // Chỉ cần ID của category
    private boolean isActive = true;
}
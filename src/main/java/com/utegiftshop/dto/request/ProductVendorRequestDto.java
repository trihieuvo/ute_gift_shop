package com.utegiftshop.dto.request;

import java.math.BigDecimal;
import java.util.List; // THÊM IMPORT

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductVendorRequestDto {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    // private String imageUrl; // <-- THAY THẾ DÒNG NÀY
    private List<String> imageUrls; // <-- BẰNG DÒNG NÀY
    private Integer categoryId;
    private boolean isActive = true;
}
package com.utegiftshop.dto.response;

import com.utegiftshop.entity.Product;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductVendorDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stockQuantity;
    private String imageUrl;
    private boolean isActive;
    private String categoryName;

    public ProductVendorDto(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.price = product.getPrice();
        this.stockQuantity = product.getStockQuantity();
        this.imageUrl = product.getImageUrl();
        this.isActive = product.isActive();
        if (product.getCategory() != null) {
            this.categoryName = product.getCategory().getName();
        } else {
            this.categoryName = "N/A";
        }
    }
}
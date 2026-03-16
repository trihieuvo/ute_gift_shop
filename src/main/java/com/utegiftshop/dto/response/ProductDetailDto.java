package com.utegiftshop.dto.response;

import com.utegiftshop.entity.Product;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter @Setter @NoArgsConstructor
public class ProductDetailDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private boolean isActive;
    private CategoryBasicDto category;
    private ShopBasicDto shop;
    private List<ProductImageDto> images;

    // Thay đổi Access Modifier từ public sang private
    private ProductDetailDto(Product product) {
        if (product == null) return;

        this.id = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.stockQuantity = product.getStockQuantity();
        this.isActive = product.isActive();

        this.category = new CategoryBasicDto(product.getCategory());
        this.shop = new ShopBasicDto(product.getShop());

        if (product.getImages() != null && !product.getImages().isEmpty()) {
            this.images = product.getImages().stream()
                              .map(ProductImageDto::new)
                              .collect(Collectors.toList());
        } else {
            this.images = Collections.emptyList();
        }
    }

    // Constructor phục vụ cho Builder (private)
    private ProductDetailDto(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.price = builder.price;
        this.stockQuantity = builder.stockQuantity;
        this.isActive = builder.isActive;
        this.category = builder.category;
        this.shop = builder.shop;
        this.images = builder.images;
    }

    // Phương thức builder() tĩnh để khởi tạo Builder
    public static Builder builder() {
        return new Builder();
    }

    // Static Inner Class Builder
    public static class Builder {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private Integer stockQuantity;
        private boolean isActive;
        private CategoryBasicDto category;
        private ShopBasicDto shop;
        private List<ProductImageDto> images;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public Builder stockQuantity(Integer stockQuantity) {
            this.stockQuantity = stockQuantity;
            return this;
        }

        public Builder isActive(boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public Builder category(CategoryBasicDto category) {
            this.category = category;
            return this;
        }

        public Builder shop(ShopBasicDto shop) {
            this.shop = shop;
            return this;
        }

        public Builder images(List<ProductImageDto> images) {
            this.images = images;
            return this;
        }

        public ProductDetailDto build() {
            return new ProductDetailDto(this);
        }
    }
}
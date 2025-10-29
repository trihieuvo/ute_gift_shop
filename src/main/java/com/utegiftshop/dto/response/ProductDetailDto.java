package com.utegiftshop.dto.response;

import com.utegiftshop.entity.Product;
import com.utegiftshop.entity.ProductImage;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ProductDetailDto {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private List<String> imageUrls;
    private ShopInfo shop;
    private CategoryInfo category;

    @Getter
    @Setter
    static class ShopInfo {
        private Long id;
        private String name;
        private Long userId; // Đây là thông tin quan trọng để chat

        ShopInfo(com.utegiftshop.entity.Shop shopEntity) {
            this.id = shopEntity.getId();
            this.name = shopEntity.getName();
            this.userId = shopEntity.getUser().getId();
        }
    }

    @Getter
    @Setter
    static class CategoryInfo {
        private Integer id;
        private String name;

        CategoryInfo(com.utegiftshop.entity.Category categoryEntity) {
            this.id = categoryEntity.getId();
            this.name = categoryEntity.getName();
        }
    }

    public ProductDetailDto(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.stockQuantity = product.getStockQuantity();
        
        if (product.getImages() != null) {
            this.imageUrls = product.getImages().stream()
                .map(ProductImage::getImageUrl)
                .collect(Collectors.toList());
        }
        
        if (product.getShop() != null) {
            this.shop = new ShopInfo(product.getShop());
        }
        
        if (product.getCategory() != null) {
            this.category = new CategoryInfo(product.getCategory());
        }
    }
}
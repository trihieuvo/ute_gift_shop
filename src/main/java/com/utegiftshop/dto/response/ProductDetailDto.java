package com.utegiftshop.dto.response;

import com.utegiftshop.entity.Product;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Collections; // Import thêm
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
    private CategoryBasicDto category; // DTO Category cơ bản
    private ShopBasicDto shop;         // DTO Shop cơ bản (chứa UserInfoBasicDto)
    private List<ProductImageDto> images; // Danh sách DTO hình ảnh

    public ProductDetailDto(Product product) {
        if (product == null) return; // Tránh NullPointerException

        this.id = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.stockQuantity = product.getStockQuantity();
        this.isActive = product.isActive();

        // Tạo DTO cho Category (kiểm tra null)
        this.category = new CategoryBasicDto(product.getCategory());

        // Tạo DTO cho Shop (kiểm tra null)
        this.shop = new ShopBasicDto(product.getShop());

        // Tạo danh sách DTO cho Images (kiểm tra null và rỗng)
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            this.images = product.getImages().stream()
                              .map(ProductImageDto::new)
                              .collect(Collectors.toList());
        } else {
            this.images = Collections.emptyList(); // Trả về danh sách rỗng thay vì null
        }
    }
}
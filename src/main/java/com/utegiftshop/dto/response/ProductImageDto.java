package com.utegiftshop.dto.response;

import com.utegiftshop.entity.ProductImage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class ProductImageDto {
    private Long id;
    private String imageUrl;

    public ProductImageDto(ProductImage img) {
        if (img != null) {
            this.id = img.getId();
            this.imageUrl = img.getImageUrl();
        }
    }
}
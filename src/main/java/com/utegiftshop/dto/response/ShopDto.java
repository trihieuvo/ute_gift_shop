package com.utegiftshop.dto.response;

import com.utegiftshop.entity.Shop;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShopDto {
    private Long id;
    private String name;
    private String description;
    // private String logoUrl; // Bỏ qua logo
    private String status;
    private String contactPhone;
    private String contactEmail;
    private String addressDetail;
    private String facebookUrl;
    private String instagramUrl;

    public ShopDto(Shop shop) {
        this.id = shop.getId();
        this.name = shop.getName();
        this.description = shop.getDescription();
        // this.logoUrl = shop.getLogoUrl(); // Bỏ qua logo
        this.status = shop.getStatus();

    }
}
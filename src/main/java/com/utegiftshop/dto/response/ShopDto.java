package com.utegiftshop.dto.response;

import com.utegiftshop.entity.Shop;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.sql.Timestamp; // Import Timestamp

@Getter
@Setter
@NoArgsConstructor
public class ShopDto {
    private Long id;
    private String name;
    private String description;
    // private String logoUrl; // Bỏ qua logo
    private String status;
    private Timestamp createdAt; // Thêm createdAt

    // --- THÊM CÁC TRƯỜNG LIÊN HỆ ---
    private String contactPhone;
    private String contactEmail;
    private String addressDetail;
    private String facebookUrl;
    private String instagramUrl;
    // --- KẾT THÚC THÊM ---

    public ShopDto(Shop shop) {
        this.id = shop.getId();
        this.name = shop.getName();
        this.description = shop.getDescription();
        // this.logoUrl = shop.getLogoUrl(); // Bỏ qua logo
        this.status = shop.getStatus();
        this.createdAt = shop.getCreatedAt(); // Gán createdAt

        // --- GÁN GIÁ TRỊ CÁC TRƯỜNG LIÊN HỆ ---
        this.contactPhone = shop.getContactPhone();
        this.contactEmail = shop.getContactEmail();
        this.addressDetail = shop.getAddressDetail();
        this.facebookUrl = shop.getFacebookUrl();
        this.instagramUrl = shop.getInstagramUrl();
        // --- KẾT THÚC GÁN ---
    }
}
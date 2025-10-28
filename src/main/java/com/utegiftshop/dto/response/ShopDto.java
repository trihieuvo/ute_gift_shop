package com.utegiftshop.dto.response;

import java.math.BigDecimal;
import java.sql.Timestamp;

import com.utegiftshop.entity.Shop;
import com.utegiftshop.entity.User;

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
    private String logoUrl;
    private String status;  
    // === BỔ SUNG: Trường Chiết khấu (Hoa hồng) ===
    private BigDecimal commissionRate; 
    
    private Timestamp createdAt; 
    private UserInfoResponse user; 


     private String contactPhone;
    private String contactEmail;
    private String addressDetail;
    private String facebookUrl;
    private String instagramUrl;

    // === HÀM KHỞI TẠO ĐÃ SỬA LỖI & THÊM CHIẾT KHẤU ===
    public ShopDto(Shop shop) {
        this.id = shop.getId();
        this.name = shop.getName();
        this.description = shop.getDescription();
        this.logoUrl = shop.getLogoUrl();
        this.status = shop.getStatus();
        // Lấy Chiết khấu từ Entity Shop (đã được thêm vào Entity ở bước trước)
        this.commissionRate = shop.getCommissionRate(); 
        this.createdAt = shop.getCreatedAt(); 
        // --- GÁN GIÁ TRỊ CÁC TRƯỜNG LIÊN HỆ ---
        this.contactPhone = shop.getContactPhone();
        this.contactEmail = shop.getContactEmail();
        this.addressDetail = shop.getAddressDetail();
        this.facebookUrl = shop.getFacebookUrl();
        this.instagramUrl = shop.getInstagramUrl();
        // === Dùng đúng constructor 6 tham số của UserInfoResponse ===
        if (shop.getUser() != null) {
            User userEntity = shop.getUser();
            
            // Gọi chính xác constructor 6 tham số
            this.user = new UserInfoResponse(
                true, // authenticated
                userEntity.getId(), 
                userEntity.getEmail(),
                userEntity.getFullName(), 
                userEntity.getPhoneNumber(),
                userEntity.getRole().getName(), 
                userEntity.getAvatarUrl() // Thêm avatarUrl
            );
        }
    }
}
package com.utegiftshop.dto.response;

import com.utegiftshop.entity.Shop;
import com.utegiftshop.entity.User; // Cần import User
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.sql.Timestamp; // Thêm import này

@Getter
@Setter
@NoArgsConstructor
public class ShopDto {
    private Long id;
    private String name;
    private String description;
    private String logoUrl; // Entity của bạn CÓ trường này
    private String status;
    
    // THÊM: Trường này RẤT CẦN THIẾT cho file HTML/JS của bạn
    private Timestamp createdAt; 

    // Trường user quan trọng cho HTML/JS
    private UserInfoResponse user; 

    // === HÀM KHỞI TẠO ĐÃ SỬA LỖI ===
    public ShopDto(Shop shop) {
        this.id = shop.getId();
        this.name = shop.getName();
        this.description = shop.getDescription();
        this.logoUrl = shop.getLogoUrl(); // Lấy logoUrl từ entity
        this.status = shop.getStatus();
        
        // THÊM: Lấy createdAt từ entity
        this.createdAt = shop.getCreatedAt(); 

        // === Dùng đúng constructor 6 tham số của UserInfoResponse ===
        if (shop.getUser() != null) {
            User userEntity = shop.getUser();
            
            // Gọi chính xác constructor 6 tham số bạn đã cung cấp
            this.user = new UserInfoResponse(
                true, // authenticated
                userEntity.getId(), 
                userEntity.getEmail(),
                userEntity.getFullName(), 
                userEntity.getPhoneNumber(),
                userEntity.getRole().getName() 
            );
        }
    }
}
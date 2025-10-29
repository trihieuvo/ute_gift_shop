package com.utegiftshop.dto.response;

import com.utegiftshop.entity.Shop;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class ShopBasicDto {
    private Long id;
    private String name;
    private UserInfoBasicDto user; // Chứa thông tin cơ bản của User (Vendor)

    public ShopBasicDto(Shop shop) {
        if (shop != null) {
            this.id = shop.getId();
            this.name = shop.getName();
            // Tạo DTO lồng nhau cho User, chỉ lấy thông tin cần thiết
            this.user = new UserInfoBasicDto(shop.getUser());
        }
    }
}
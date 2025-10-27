package com.utegiftshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShopRegistrationRequest {
    @NotBlank
    private String name;
    private String description;
    // private String logoUrl;
    // Thêm các trường khác mà form đăng ký yêu cầu
}
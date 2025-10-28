package com.utegiftshop.dto.request;

import jakarta.validation.constraints.Email; // Thêm import Email
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateShopRequestDto {

    @Size(max = 255, message = "Tên cửa hàng không quá 255 ký tự")
    private String name;

    @Size(max = 1000, message = "Mô tả không quá 1000 ký tự")
    private String description;
    // private String logoUrl; // Bỏ qua logo

    // --- THÊM CÁC TRƯỜNG LIÊN HỆ ---
    @Size(max = 20, message = "Số điện thoại không quá 20 ký tự")
    private String contactPhone;

    @Email(message = "Email liên hệ không hợp lệ")
    @Size(max = 255, message = "Email không quá 255 ký tự")
    private String contactEmail;

    private String addressDetail; // Không giới hạn size cụ thể, CSDL là TEXT

    @Size(max = 512, message = "URL Facebook không quá 512 ký tự")
    private String facebookUrl;

    @Size(max = 512, message = "URL Instagram không quá 512 ký tự")
    private String instagramUrl;
    // --- KẾT THÚC THÊM ---
}
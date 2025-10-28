package com.utegiftshop.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopRegistrationRequestDto {

    @NotBlank(message = "Tên cửa hàng không được để trống")
    @Size(max = 255, message = "Tên cửa hàng không quá 255 ký tự")
    private String name;

    @Size(max = 1000, message = "Mô tả không quá 1000 ký tự")
    private String description;

    @Size(max = 20, message = "Số điện thoại không quá 20 ký tự")
    private String contactPhone;

    @Email(message = "Email liên hệ không hợp lệ")
    @Size(max = 255, message = "Email không quá 255 ký tự")
    private String contactEmail;

    private String addressDetail;

    @Size(max = 512, message = "URL Facebook không quá 512 ký tự")
    private String facebookUrl;

    @Size(max = 512, message = "URL Instagram không quá 512 ký tự")
    private String instagramUrl;
}
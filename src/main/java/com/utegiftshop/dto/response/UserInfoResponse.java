package com.utegiftshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor // Giúp tạo constructor nhanh chóng
public class UserInfoResponse {
    private boolean authenticated;
    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String role;
}
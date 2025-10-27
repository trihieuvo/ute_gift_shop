package com.utegiftshop.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleApplicationRequest {
    private String requestedRole; // "Vendor" hoặc "Shipper"
    private String message;
}
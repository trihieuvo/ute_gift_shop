package com.utegiftshop.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateShopRequestDto {

    @Size(max = 255)
    private String name;
    private String description;
    // private String logoUrl; // Bỏ qua logo
 
}
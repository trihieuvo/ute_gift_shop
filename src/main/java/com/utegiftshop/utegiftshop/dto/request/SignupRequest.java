package com.utegiftshop.dto.request;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SignupRequest {
    private String fullName;
    private String email;
    private String password;
}
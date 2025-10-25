package com.utegiftshop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebViewController {

    // Trả về trang login (sẽ tương ứng với login.html)
    @GetMapping("/login")
    public String loginPage() {
        return "login"; 
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    // Đổi tên "active" thành "activate" để khớp với các file trước
    @GetMapping("/activate")
    public String activatePage() {
        return "activate"; 
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage() {
        return "reset-password";
    }

    // Trang chủ sau khi đăng nhập
    @GetMapping("/home")
    public String homePage() {
        return "home";
    }
    
    // Chuyển hướng trang gốc "/" về "/home"
    @GetMapping("/")
    public String rootPage() {
        return "redirect:/home";
    }
}
package com.utegiftshop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Ánh xạ URL tới tên của tệp template (không cần .html)
        
        // Trang chính
        registry.addViewController("/home").setViewName("home");

        // Các trang xác thực
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/signup").setViewName("signup");
        registry.addViewController("/activate").setViewName("activate");
        registry.addViewController("/forgot-password").setViewName("forgot-password");
        registry.addViewController("/reset-password").setViewName("reset-password");

        // Bạn cũng có thể thêm một trang chủ mặc định
        // registry.addViewController("/").setViewName("home"); 
        // Hoặc trỏ về login nếu chưa đăng nhập (Security sẽ xử lý việc này)
        registry.addViewController("/").setViewName("login");
    }
}

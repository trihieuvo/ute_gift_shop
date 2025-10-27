package com.utegiftshop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
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

        registry.addViewController("/cart").setViewName("cart"); // Trang giỏ hàng
        registry.addViewController("/checkout").setViewName("checkout"); // Trang thanh toán
        registry.addViewController("/profile").setViewName("profile"); // Trang thông tin cá nhân

        // === BỔ SUNG: ÁNH XẠ CHO LỊCH SỬ ĐƠN HÀNG ===
        registry.addViewController("/order-history").setViewName("order-history");
        // Ánh xạ cho trang chi tiết, {id} sẽ được xử lý bởi JS ở frontend
        registry.addViewController("/orders/{id}").setViewName("order-details"); 
        // === KẾT THÚC BỔ SUNG ===

        // BỎ CÁC VIEW CONTROLLER CHO /shipper/... ở đây nếu có

        registry.addViewController("/").setViewName("login");
    }
 
}
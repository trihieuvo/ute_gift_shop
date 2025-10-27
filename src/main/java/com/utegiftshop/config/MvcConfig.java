package com.utegiftshop.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(MvcConfig.class);

    @Value("${app.upload.dir}") // Lấy đường dẫn thư mục upload từ application.properties
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

            // Tự động tạo thư mục nếu chưa có
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("Upload directory created at: {}", uploadPath.toString());
            } else if (!Files.isDirectory(uploadPath)) {
                 logger.error("Configured upload path is not a directory: {}", uploadPath.toString());
                 return; // Bỏ qua nếu cấu hình sai
            }

            // Chuyển đường dẫn thành dạng URI hợp lệ (vd: file:/C:/...)
            String uploadLocationUri = uploadPath.toUri().toString();

            logger.info("Mapping URL path /uploaded-images/** to location: {}", uploadLocationUri);

            // Đăng ký resource handler
            registry.addResourceHandler("/uploaded-images/**") // URL pattern trình duyệt gọi
                    .addResourceLocations(uploadLocationUri); // Đường dẫn thư mục vật lý

        } catch (IOException e) {
            logger.error("Could not create or access upload directory: {}", uploadDir, e);
        } catch (Exception e) {
             logger.error("Error configuring resource handler for upload directory: {}", uploadDir, e);
        }
    }
 
}
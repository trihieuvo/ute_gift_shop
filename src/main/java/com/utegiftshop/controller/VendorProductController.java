package com.utegiftshop.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/vendor/products")
public class VendorProductController {

    private static final Logger logger = LoggerFactory.getLogger(VendorProductController.class);

    // Đường dẫn này sẽ được inject từ file application.properties
    @Value("${app.upload.dir}")
    private String uploadDir;

    // ... các method GET, PUT, DELETE, POST khác của bạn ...

    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        // 1. Kiểm tra file có rỗng không
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng chọn một file để upload."));
        }

        // 2. Kiểm tra định dạng file (chỉ chấp nhận ảnh)
        if (!Objects.requireNonNull(file.getContentType()).startsWith("image/")) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body(Map.of("message", "File không phải là hình ảnh hợp lệ."));
        }

        try {
            // 3. Lấy tên file gốc và làm sạch
            String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

            // Lấy đuôi file một cách an toàn
            String extension = "";
            int lastDot = originalFilename.lastIndexOf('.');
            if (lastDot > 0) {
                extension = originalFilename.substring(lastDot);
            }

            // 4. Tạo tên file unique để tránh trùng lặp
            String newFilename = UUID.randomUUID().toString() + extension;

            // 5. Tạo đường dẫn đến thư mục upload
            Path uploadPath = Paths.get(uploadDir);

            // 6. Đảm bảo thư mục tồn tại, nếu không thì tạo mới
            if (!Files.exists(uploadPath)) {
                logger.info("Creating directory: {}", uploadPath);
                Files.createDirectories(uploadPath);
            }

            // 7. Tạo đường dẫn đầy đủ đến file và lưu file
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Saved file to: {}", filePath);

            // 8. Trả về URL với timestamp để tránh cache
            // ✅ QUAN TRỌNG: Thêm ?t=timestamp vào URL
            String imageUrl = "/images/" + newFilename + "?t=" + System.currentTimeMillis();
            
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error("Lỗi khi lưu file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi máy chủ khi lưu file."));
        }
    }
}
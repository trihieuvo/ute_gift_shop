package com.utegiftshop.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.utegiftshop.dto.request.ChangePasswordRequest;
import com.utegiftshop.dto.request.UpdateProfileRequest;
import com.utegiftshop.dto.response.UserInfoResponse;
import com.utegiftshop.entity.User;
import com.utegiftshop.repository.UserRepository;
import com.utegiftshop.security.service.UserDetailsImpl;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.upload.dir}")
    private String uploadDir;

    /**
     * Lấy thông tin user hiện tại (ĐÃ CẬP NHẬT)
     */
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User currentUser = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy người dùng trong CSDL."));

        String role = userDetails.getAuthorities().stream()
                                  .map(GrantedAuthority::getAuthority)
                                  .collect(Collectors.joining());

        // Tạo đối tượng DTO để trả về (đã thêm avatarUrl)
        UserInfoResponse response = new UserInfoResponse(
            true,
            currentUser.getId(),
            currentUser.getEmail(),
            currentUser.getFullName(),
            currentUser.getPhoneNumber(),
            role,
            currentUser.getAvatarUrl() // <-- Trả về avatarUrl
        );

        return ResponseEntity.ok(response);
    }

    /**
     * API để cập nhật thông tin cá nhân (Họ tên, SĐT)
     */
    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        userRepository.save(user);

        return ResponseEntity.ok("Cập nhật thông tin thành công.");
    }

    /**
     * API để đổi mật khẩu
     */
    @PostMapping("/me/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("Mật khẩu hiện tại không chính xác.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok("Đổi mật khẩu thành công.");
    }

    /**
     * API MỚI: UPLOAD AVATAR
     */
    @PostMapping("/me/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        // 1. Kiểm tra file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng chọn file avatar."));
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body(Map.of("message", "File không phải là hình ảnh hợp lệ."));
        }
        // Giới hạn kích thước file (ví dụ: 2MB sau khi crop)
        if (file.getSize() > 2 * 1024 * 1024) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Map.of("message", "Kích thước file avatar không được vượt quá 2MB."));
        }

        try {
            // 2. Tạo tên file unique
            String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            String extension = "";
            int lastDot = originalFilename.lastIndexOf('.');
            if (lastDot > 0) {
                extension = originalFilename.substring(lastDot);
            } else {
                // Fallback extension if needed, based on content type
                if (contentType.equals("image/png")) extension = ".png";
                else if (contentType.equals("image/jpeg")) extension = ".jpg";
                else if (contentType.equals("image/gif")) extension = ".gif";
                else if (contentType.equals("image/webp")) extension = ".webp";
                else extension = ".img"; // Generic fallback
            }
            String newFilename = UUID.randomUUID().toString() + extension;

            // 3. Lưu file
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                logger.info("Creating directory for avatars: {}", uploadPath);
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Saved avatar for user {} to: {}", userId, filePath);

            // 4. Cập nhật User entity
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found during avatar update"));

            String imageUrl = "/images/" + newFilename; // URL để truy cập từ frontend
            user.setAvatarUrl(imageUrl);
            userRepository.save(user);

            // 5. Trả về thông tin cập nhật
            Map<String, String> response = new HashMap<>();
            response.put("message", "Cập nhật avatar thành công.");
            response.put("avatarUrl", imageUrl); // Trả về URL mới (không có timestamp ở đây)

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error("Lỗi khi lưu file avatar cho user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi máy chủ khi lưu file avatar."));
        } catch (RuntimeException e) {
             logger.error("Lỗi khi cập nhật avatar cho user {}: {}", userId, e.getMessage(), e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR) // Use 500 for runtime issues
                     .body(Map.of("message", "Đã xảy ra lỗi khi cập nhật avatar: " + e.getMessage()));
        }
    }
}
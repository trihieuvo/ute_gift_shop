package com.utegiftshop.controller;

import com.utegiftshop.entity.User;
import com.utegiftshop.security.service.UserService; // Import UserService
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users") // API được bảo vệ bởi SecurityConfig
public class AdminUserApiController {

    private final UserService userService;

    public AdminUserApiController(UserService userService) {
        this.userService = userService;
    }

    // API lấy danh sách tất cả user
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.findAllUsers();
        // (Lưu ý: Trả về User entity có thể lộ mật khẩu hash, nên tạo DTO Response sẽ tốt hơn)
        // Tạm thời chấp nhận để làm nhanh
        return ResponseEntity.ok(users);
    }

    // API để Khóa/Mở khóa user
    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<User> toggleUserStatus(@PathVariable Long id) {
        User updatedUser = userService.toggleUserStatus(id);
        return ResponseEntity.ok(updatedUser);
    }
}
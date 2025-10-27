package com.utegiftshop.controller;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity; // THÊM IMPORT MỚI
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utegiftshop.dto.request.ChangePasswordRequest;
import com.utegiftshop.dto.request.UpdateProfileRequest;
import com.utegiftshop.dto.response.UserInfoResponse;
import com.utegiftshop.entity.User;
import com.utegiftshop.repository.UserRepository;
import com.utegiftshop.security.service.UserDetailsImpl;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * SỬA LỖI: Trả về một DTO an toàn thay vì Map hoặc Entity
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

        // Tạo đối tượng DTO để trả về
        UserInfoResponse response = new UserInfoResponse(
            true,
            currentUser.getId(),
            currentUser.getEmail(),
            currentUser.getFullName(),
            currentUser.getPhoneNumber(),
            role
        );

        return ResponseEntity.ok(response);
    }

    /**
     * API để cập nhật thông tin cá nhân (Giữ nguyên)
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
     * API để đổi mật khẩu (Giữ nguyên)
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
}
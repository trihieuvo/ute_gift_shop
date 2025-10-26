package com.utegiftshop.controller;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder; // Import thêm
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utegiftshop.entity.User;
import com.utegiftshop.repository.UserRepository; // Import thêm
import com.utegiftshop.security.service.UserDetailsImpl;


// *** TẠO CONTROLLER NÀY ĐỂ XỬ LÝ YÊU CẦU LẤY THÔNG TIN USER HIỆN TẠI ***
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    UserRepository userRepository; // Inject UserRepository

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // KIỂM TRA NẾU CHƯA XÁC THỰC HOẶC LÀ NGƯỜI DÙNG VÔ DANH (GUEST)
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            // Thay vì trả về lỗi, trả về một đối tượng rỗng hoặc một thông báo trạng thái
            // Trả về 200 OK để frontend không coi đây là một lỗi
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        // NẾU ĐÃ XÁC THỰC, TRẢ VỀ THÔNG TIN USER NHƯ CŨ
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        String fullName = userRepository.findById(userDetails.getId())
                                      .map(User::getFullName)
                                      .orElse("N/A");

        String role = userDetails.getAuthorities().stream()
                                  .map(GrantedAuthority::getAuthority)
                                  .collect(Collectors.joining());

        return ResponseEntity.ok(Map.of(
            "authenticated", true, // Thêm trường này để frontend dễ kiểm tra
            "id", userDetails.getId(),
            "email", userDetails.getUsername(),
            "fullName", fullName,
            "role", role
        ));
    }
}
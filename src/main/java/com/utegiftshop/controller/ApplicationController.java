package com.utegiftshop.controller;

import com.utegiftshop.dto.request.RoleApplicationRequest;
import com.utegiftshop.entity.RoleApplication;
import com.utegiftshop.entity.User;
import com.utegiftshop.repository.RoleApplicationRepository;
import com.utegiftshop.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/apply")
@PreAuthorize("hasAuthority('Customer')") // Chỉ Customer mới được apply
public class ApplicationController {

    @Autowired
    private RoleApplicationRepository applicationRepository;

    private UserDetailsImpl getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserDetailsImpl) authentication.getPrincipal();
    }

    @PostMapping("/role")
    public ResponseEntity<?> applyForRole(@RequestBody RoleApplicationRequest request) {
        UserDetailsImpl userDetails = getCurrentUser();

        // Kiểm tra xem user đã có đơn nào đang chờ duyệt chưa
        if (applicationRepository.existsByUserIdAndStatus(userDetails.getId(), "PENDING")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Bạn đã có một đơn đăng ký đang chờ duyệt."));
        }

        // Kiểm tra vai trò hợp lệ
        String role = request.getRequestedRole();
        if (!"Vendor".equals(role) && !"Shipper".equals(role)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vai trò yêu cầu không hợp lệ."));
        }

        RoleApplication application = new RoleApplication();
        User user = new User();
        user.setId(userDetails.getId());
        application.setUser(user);
        application.setRequestedRole(role);
        application.setMessage(request.getMessage());
        application.setStatus("PENDING");
        application.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        application.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        applicationRepository.save(application);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Đã gửi đơn đăng ký. Vui lòng chờ Admin phê duyệt."));
    }

    @GetMapping("/my-status")
    public ResponseEntity<?> getMyApplicationStatus() {
        UserDetailsImpl userDetails = getCurrentUser();

        Optional<RoleApplication> appOpt = applicationRepository
                .findByUserIdAndStatus(userDetails.getId(), "PENDING");

        if (appOpt.isPresent()) {
            return ResponseEntity.ok(Map.of(
                "hasPendingApplication", true,
                "status", appOpt.get().getStatus(),
                "requestedRole", appOpt.get().getRequestedRole()
            ));
        }

        return ResponseEntity.ok(Map.of("hasPendingApplication", false));
    }
}
package com.utegiftshop.controller;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.utegiftshop.dto.response.RoleApplicationDto;
import com.utegiftshop.entity.Role;
import com.utegiftshop.entity.RoleApplication;
import com.utegiftshop.entity.Shop;
import com.utegiftshop.entity.User;
import com.utegiftshop.repository.RoleApplicationRepository;
import com.utegiftshop.repository.RoleRepository;
import com.utegiftshop.repository.ShopRepository;
import com.utegiftshop.repository.UserRepository;

@RestController
@RequestMapping("/api/admin/applications")
public class AdminApplicationController {

    @Autowired private RoleApplicationRepository applicationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private ShopRepository shopRepository;

    @GetMapping
    public ResponseEntity<List<RoleApplicationDto>> getPendingApplications(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(required = false) String requestedRole) { // Thêm tham số này

        List<RoleApplication> applications = applicationRepository.findByStatus(status.toUpperCase());

        // Nếu có yêu cầu lọc theo vai trò, lọc kết quả
        if (requestedRole != null && !requestedRole.isEmpty()) {
            List<RoleApplicationDto> dtos = applications.stream()
                    .filter(app -> requestedRole.equalsIgnoreCase(app.getRequestedRole())) // Lọc ở đây
                    .map(RoleApplicationDto::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        }

        // Giữ nguyên logic cũ nếu không lọc theo vai trò
        List<RoleApplicationDto> dtos = applications.stream()
                .map(RoleApplicationDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<?> approveApplication(@PathVariable Long id) {
        RoleApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đăng ký."));

        if (!"PENDING".equals(application.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Đơn này đã được xử lý."));
        }

        User user = application.getUser();
        String requestedRoleName = application.getRequestedRole(); // "Vendor" hoặc "Shipper"

        Role newRole = roleRepository.findByName(requestedRoleName)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vai trò: " + requestedRoleName));

        // 1. Cập nhật vai trò (Role) cho User
        user.setRole(newRole);
        userRepository.save(user);

        // 2. Nếu là Vendor, tạo một Shop mới cho họ
        if ("Vendor".equals(requestedRoleName)) {
            if (shopRepository.findByUserId(user.getId()).isEmpty()) {
                Shop newShop = new Shop();
                newShop.setUser(user);
                newShop.setName(user.getFullName() + "'s Shop"); // Tên mặc định
                newShop.setDescription("Chào mừng đến với cửa hàng của tôi!");
                newShop.setStatus("ACTIVE"); // Kích hoạt luôn
                newShop.setCreatedAt(new Timestamp(System.currentTimeMillis()));
                newShop.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
                shopRepository.save(newShop);
            }
        }

        // 3. Cập nhật trạng thái đơn đăng ký
        application.setStatus("APPROVED");
        application.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        applicationRepository.save(application);

        // (Tùy chọn: Gửi email thông báo cho user)

        return ResponseEntity.ok(Map.of("message", "Đã phê duyệt thành công."));
    }

    @PostMapping("/{id}/reject")
    @Transactional
    public ResponseEntity<?> rejectApplication(@PathVariable Long id) {
        RoleApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đăng ký."));

        if (!"PENDING".equals(application.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Đơn này đã được xử lý."));
        }

        application.setStatus("REJECTED");
        application.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        applicationRepository.save(application);

        // (Tùy chọn: Gửi email thông báo từ chối cho user)

        return ResponseEntity.ok(Map.of("message", "Đã từ chối đơn đăng ký."));
    }
}
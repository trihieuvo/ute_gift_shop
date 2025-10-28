package com.utegiftshop.controller;

import com.utegiftshop.dto.request.ShopRegistrationRequestDto; // Import DTO mới
import com.utegiftshop.dto.request.UpdateShopRequestDto;
import com.utegiftshop.dto.response.ShopDto;
import com.utegiftshop.entity.Shop;
import com.utegiftshop.entity.User; // Import User
import com.utegiftshop.repository.ShopRepository;
import com.utegiftshop.security.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.sql.Timestamp; // Import Timestamp
import java.util.Map;
import java.util.Optional; // Import Optional

@RestController
@RequestMapping("/api/vendor/shop")
@PreAuthorize("hasAuthority('Vendor')") // Đảm bảo chỉ Vendor mới gọi được API này
public class VendorShopController {

    private static final Logger logger = LoggerFactory.getLogger(VendorShopController.class);

    @Autowired
    private ShopRepository shopRepository;

    // --- Helper lấy UserDetails ---
    private UserDetailsImpl getAuthenticatedUserDetails(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("Xác thực lỗi.");
        }
        return userDetails;
    }

    // --- Helper lấy Shop (Chỉ dùng khi shop ĐÃ tồn tại) ---
    private Shop getExistingShop(Long userId) {
        return shopRepository.findByUserId(userId)
                .orElseThrow(() -> {
                     logger.error("Shop Not Found user ID: {}", userId);
                     return new RuntimeException("Cửa hàng chưa được thiết lập.");
                 });
    }

    /**
     * GET /api/vendor/shop/status : Kiểm tra trạng thái cửa hàng của Vendor hiện tại
     * Trả về: { status: "ACTIVE" / "PENDING" / "REJECTED" / "NOT_FOUND" }
     */
    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getShopStatus(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            UserDetailsImpl authUser = getAuthenticatedUserDetails(userDetails);
            Optional<Shop> shopOpt = shopRepository.findByUserId(authUser.getId());

            if (shopOpt.isPresent()) {
                logger.info("[getShopStatus] Found Shop ID: {} with Status: {} for User ID: {}",
                            shopOpt.get().getId(), shopOpt.get().getStatus(), authUser.getId());
                return ResponseEntity.ok(Map.of("status", shopOpt.get().getStatus()));
            } else {
                logger.info("[getShopStatus] No Shop found for User ID: {}", authUser.getId());
                return ResponseEntity.ok(Map.of("status", "NOT_FOUND"));
            }
        } catch (Exception e) {
            logger.error("[getShopStatus] Unexpected error:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(Map.of("message", "Lỗi máy chủ khi kiểm tra trạng thái cửa hàng."));
        }
    }


    /**
     * POST /api/vendor/shop/register : Đăng ký cửa hàng mới
     */
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> registerShop(@Valid @RequestBody ShopRegistrationRequestDto request,
                                         @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            UserDetailsImpl authUser = getAuthenticatedUserDetails(userDetails);
            Long userId = authUser.getId();

            // Kiểm tra xem user đã có shop chưa
            if (shopRepository.findByUserId(userId).isPresent()) {
                logger.warn("[registerShop] User ID: {} already has a shop. Registration denied.", userId);
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .body(Map.of("message", "Bạn đã có cửa hàng hoặc đang chờ duyệt."));
            }

            logger.info("[registerShop] Registering new shop for User ID: {}", userId);

            Shop newShop = new Shop();
            User userRef = new User(); // Chỉ cần ID để tạo liên kết
            userRef.setId(userId);
            newShop.setUser(userRef);

            // Gán thông tin từ request
            newShop.setName(request.getName());
            newShop.setDescription(request.getDescription());
            newShop.setContactPhone(request.getContactPhone());
            newShop.setContactEmail(request.getContactEmail());
            newShop.setAddressDetail(request.getAddressDetail());
            newShop.setFacebookUrl(request.getFacebookUrl());
            newShop.setInstagramUrl(request.getInstagramUrl());

            // Đặt trạng thái chờ duyệt
            newShop.setStatus("PENDING");
            newShop.setCreatedAt(new Timestamp(System.currentTimeMillis())); // Set ngày tạo
            newShop.setUpdatedAt(new Timestamp(System.currentTimeMillis())); // Set ngày cập nhật ban đầu

            Shop savedShop = shopRepository.save(newShop);
            logger.info("[registerShop] Shop registered successfully with ID: {} and Status: PENDING", savedShop.getId());

            // Trả về thông tin shop vừa tạo (dùng ShopDto) và status 201 Created
            return ResponseEntity.status(HttpStatus.CREATED).body(new ShopDto(savedShop));

        } catch (Exception e) { // Bắt lỗi chung, bao gồm cả validation
            logger.error("[registerShop] Error during shop registration:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(Map.of("message", "Lỗi máy chủ khi đăng ký cửa hàng: " + e.getMessage()));
        }
    }


    // --- Các API cũ (getShopDetails, updateShopDetails) ---
    // Sửa lại cách lấy shop để dùng getExistingShop
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getShopDetails(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            UserDetailsImpl authUser = getAuthenticatedUserDetails(userDetails);
            Shop shop = getExistingShop(authUser.getId()); // Dùng helper mới
            logger.info("[getShopDetails] Found Shop ID: {} for User ID: {}", shop.getId(), authUser.getId());
            return ResponseEntity.ok(new ShopDto(shop));
        } catch (RuntimeException e) {
            logger.error("[getShopDetails] Error: {}", e.getMessage());
             // Trả về 404 nếu getExistingShop báo lỗi không tìm thấy
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("[getShopDetails] Unexpected error:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(Map.of("message", "Lỗi máy chủ."));
        }
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> updateShopDetails(@Valid @RequestBody UpdateShopRequestDto request,
                                               @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            UserDetailsImpl authUser = getAuthenticatedUserDetails(userDetails);
            Shop shop = getExistingShop(authUser.getId()); // Dùng helper mới

             // Kiểm tra nếu shop không phải ACTIVE thì không cho update (TÙY CHỌN)
            // if (!"ACTIVE".equalsIgnoreCase(shop.getStatus())) {
            //     logger.warn("[updateShopDetails] Attempt to update non-active shop ID: {}", shop.getId());
            //     return ResponseEntity.status(HttpStatus.FORBIDDEN)
            //                          .contentType(MediaType.APPLICATION_JSON)
            //                          .body(Map.of("message", "Chỉ có thể cập nhật cửa hàng đang hoạt động."));
            // }

            logger.info("[updateShopDetails] Updating Shop ID: {}", shop.getId());
            shop.setName(request.getName());
            shop.setDescription(request.getDescription());
            shop.setContactPhone(request.getContactPhone());
            shop.setContactEmail(request.getContactEmail());
            shop.setAddressDetail(request.getAddressDetail());
            shop.setFacebookUrl(request.getFacebookUrl());
            shop.setInstagramUrl(request.getInstagramUrl());
            // updatedAt tự cập nhật qua @PreUpdate

            Shop updatedShop = shopRepository.save(shop);
            logger.info("[updateShopDetails] Shop ID: {} updated.", updatedShop.getId());
            return ResponseEntity.ok(new ShopDto(updatedShop));

        } catch (RuntimeException e) {
            logger.error("[updateShopDetails] Error: {}", e.getMessage());
            HttpStatus status = e.getMessage().contains("Cửa hàng chưa được thiết lập") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("[updateShopDetails] Unexpected error:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(Map.of("message", "Lỗi máy chủ."));
        }
    }
}
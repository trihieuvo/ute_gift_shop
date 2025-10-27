package com.utegiftshop.controller;

import com.utegiftshop.dto.request.UpdateShopRequestDto;
import com.utegiftshop.dto.response.ShopDto;
import com.utegiftshop.entity.Shop;
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

// Removed Timestamp import, handled by @PreUpdate

@RestController
@RequestMapping("/api/vendor/shop") // Base path riêng
@PreAuthorize("hasAuthority('Vendor')")
public class VendorShopController {

    private static final Logger logger = LoggerFactory.getLogger(VendorShopController.class);

    @Autowired
    private ShopRepository shopRepository;

    // --- Helper lấy Shop ---
    private Shop getAuthenticatedShop(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) throw new RuntimeException("Xác thực lỗi.");
        Long userId = userDetails.getId();
        return shopRepository.findByUserId(userId)
                .orElseThrow(() -> {
                     logger.error("Shop Not Found user ID: {}", userId);
                     return new RuntimeException("Cửa hàng chưa được thiết lập.");
                 });
    }

    /**
     * GET /api/vendor/shop : Lấy thông tin cửa hàng hiện tại
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getShopDetails(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Shop shop = getAuthenticatedShop(userDetails);
            logger.info("[getShopDetails] Found Shop ID: {} for User ID: {}", shop.getId(), userDetails.getId());
            return ResponseEntity.ok(new ShopDto(shop));
        } catch (RuntimeException e) { /* 404 */ logger.error("[getShopDetails] Error: {}", e.getMessage()); return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body("{\"message\": \"" + e.getMessage() + "\"}"); }
          catch (Exception e) { /* 500 */ logger.error("[getShopDetails] Unexpected error:", e); return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body("{\"message\": \"Lỗi máy chủ.\"}"); }
    }

    /**
     * PUT /api/vendor/shop : Cập nhật thông tin cửa hàng hiện tại
     */
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> updateShopDetails(@Valid @RequestBody UpdateShopRequestDto request,
                                               @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Shop shop = getAuthenticatedShop(userDetails);
            logger.info("[updateShopDetails] Updating Shop ID: {}", shop.getId());

            // Cập nhật các trường
            shop.setName(request.getName());
            shop.setDescription(request.getDescription());
            // Bỏ qua logo
  
            // updatedAt được xử lý bởi @PreUpdate

            Shop updatedShop = shopRepository.save(shop);
            logger.info("[updateShopDetails] Shop ID: {} updated.", updatedShop.getId());

            return ResponseEntity.ok(new ShopDto(updatedShop));

        } catch (RuntimeException e) { /* 404/400 */ logger.error("[updateShopDetails] Error: {}", e.getMessage()); HttpStatus status = e.getMessage().contains("Cửa hàng") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST; return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body("{\"message\": \"" + e.getMessage() + "\"}"); }
          catch (Exception e) { /* 500 */ logger.error("[updateShopDetails] Unexpected error:", e); return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body("{\"message\": \"Lỗi máy chủ.\"}"); }
    }
}
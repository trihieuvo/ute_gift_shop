package com.utegiftshop.controller;

import com.utegiftshop.dto.request.PromotionRequestDto;
import com.utegiftshop.dto.response.PromotionDto;
import com.utegiftshop.entity.Promotion;
import com.utegiftshop.entity.Shop;
import com.utegiftshop.repository.PromotionRepository;
import com.utegiftshop.repository.ShopRepository;
import com.utegiftshop.security.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;
import java.util.List;

@RestController
@RequestMapping("/api/vendor/promotions")
@PreAuthorize("hasAuthority('Vendor')")
public class VendorPromotionController {

    private static final Logger logger = LoggerFactory.getLogger(VendorPromotionController.class);

    @Autowired private PromotionRepository promotionRepository;
    @Autowired private ShopRepository shopRepository;

    private Shop getAuthenticatedShop(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) throw new RuntimeException("Xác thực lỗi.");
        Long userId = userDetails.getId();
        return shopRepository.findByUserId(userId).orElseThrow(() -> {
            logger.error("Shop Not Found user ID: {}", userId);
            return new RuntimeException("Cửa hàng chưa được thiết lập.");
        });
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getShopPromotions(
            @RequestParam(required = false) String code, // <-- ADDED
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        try {
            Shop shop = getAuthenticatedShop(userDetails);
            List<Promotion> promotions;

            // === MODIFIED: Logic to filter by code ===
            if (code != null && !code.trim().isEmpty()) {
                logger.info("Fetching promotions for shop {} with code like '{}'", shop.getId(), code);
                promotions = promotionRepository.findByShopIdAndCodeContainingIgnoreCaseOrderByIdDesc(shop.getId(), code.trim());
            } else {
                logger.info("Fetching all promotions for shop {}", shop.getId());
                promotions = promotionRepository.findByShopIdOrderByIdDesc(shop.getId());
            }
            // === END MODIFIED ===

            List<PromotionDto> dtos = promotions.stream().map(PromotionDto::new).collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (RuntimeException e) {
            logger.error("[getShopPromotions] Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body("{\"message\":\""+e.getMessage()+"\"}");
        } catch (Exception e) {
            logger.error("[getShopPromotions] Unexpected error:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body("{\"message\":\"Lỗi server.\"}");
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> createPromotion(@Valid @RequestBody PromotionRequestDto request,
                                             @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Shop shop = getAuthenticatedShop(userDetails);
            if (request.getEndDate() == null || request.getStartDate() == null || !request.getEndDate().after(request.getStartDate())) {
                 throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu.");
            }

            Promotion promotion = new Promotion();
            promotion.setShop(shop);
            promotion.setCode(request.getCode().toUpperCase());
            promotion.setDiscountPercent(request.getDiscountPercent());
            promotion.setMinOrderValue(request.getMinOrderValue());
            promotion.setQuantity(request.getQuantity());
            promotion.setStartDate(request.getStartDate());
            promotion.setEndDate(request.getEndDate());

            Promotion savedPromotion = promotionRepository.save(promotion);
            return ResponseEntity.status(HttpStatus.CREATED).body(new PromotionDto(savedPromotion));

        } catch (DataIntegrityViolationException e) { // <-- ĐẶT LÊN TRƯỚC
             logger.error("[createPromotion] Data integrity violation (code exists?): {}", e.getMessage());
             return ResponseEntity.status(HttpStatus.CONFLICT)
                                  .contentType(MediaType.APPLICATION_JSON)
                                  .body("{\"message\":\"Mã khuyến mãi '" + request.getCode().toUpperCase() + "' đã tồn tại.\"}");
        } catch (RuntimeException e) { // <-- ĐẶT SAU
             logger.error("[createPromotion] Error: {}", e.getMessage());
             return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body("{\"message\":\""+e.getMessage()+"\"}");
        } catch (Exception e) {
            logger.error("[createPromotion] Unexpected error:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body("{\"message\":\"Lỗi máy chủ khi tạo khuyến mãi.\"}");
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> updatePromotion(@PathVariable Integer id,
                                             @Valid @RequestBody PromotionRequestDto request,
                                             @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Shop shop = getAuthenticatedShop(userDetails);
            Promotion promotion = promotionRepository.findByIdAndShopId(id, shop.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy KM (ID: " + id + ") hoặc không thuộc shop."));

            if (request.getEndDate() == null || request.getStartDate() == null || !request.getEndDate().after(request.getStartDate())) {
                 throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu.");
            }

            promotion.setCode(request.getCode().toUpperCase());
            promotion.setDiscountPercent(request.getDiscountPercent());
            promotion.setMinOrderValue(request.getMinOrderValue());
            promotion.setQuantity(request.getQuantity());
            promotion.setStartDate(request.getStartDate());
            promotion.setEndDate(request.getEndDate());

            Promotion updatedPromotion = promotionRepository.save(promotion);
            return ResponseEntity.ok(new PromotionDto(updatedPromotion));

        } catch (DataIntegrityViolationException e) { // <-- ĐẶT LÊN TRƯỚC
             logger.error("[updatePromotion] Data integrity violation ID {}: {}", id, e.getMessage());
             return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body("{\"message\":\"Mã khuyến mãi '" + request.getCode().toUpperCase() + "' đã tồn tại.\"}");
        } catch (RuntimeException e) { // <-- ĐẶT SAU
            logger.error("[updatePromotion] Error ID {}: {}", id, e.getMessage());
            HttpStatus status = e.getMessage().contains("Không tìm thấy") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body("{\"message\":\""+e.getMessage()+"\"}");
        } catch (Exception e) {
             logger.error("[updatePromotion] Unexpected error ID {}:", id, e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body("{\"message\":\"Lỗi máy chủ khi cập nhật.\"}");
        }
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> deletePromotion(@PathVariable Integer id,
                                             @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Shop shop = getAuthenticatedShop(userDetails);
            Promotion promotion = promotionRepository.findByIdAndShopId(id, shop.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy KM (ID: " + id + ") hoặc không thuộc shop."));

            promotionRepository.delete(promotion);
            return ResponseEntity.ok("{\"message\":\"Đã xóa khuyến mãi thành công.\"}");

        } catch (DataIntegrityViolationException e) { // <-- ĐẶT LÊN TRƯỚC
             logger.error("[deletePromotion] Constraint violation ID {}: {}", id, e.getMessage());
             return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body("{\"message\":\"Không thể xóa khuyến mãi đã được áp dụng hoặc liên kết.\"}");
        } catch (RuntimeException e) { // <-- ĐẶT SAU
             logger.error("[deletePromotion] Error ID {}: {}", id, e.getMessage());
             return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body("{\"message\":\""+e.getMessage()+"\"}");
        } catch (Exception e) {
             logger.error("[deletePromotion] Unexpected error ID {}:", id, e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body("{\"message\":\"Lỗi máy chủ khi xóa.\"}");
        }
    }
}
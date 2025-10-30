package com.utegiftshop.controller;

import java.sql.Timestamp; // SỬA: Import DTO
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utegiftshop.dto.response.PromotionDto;
import com.utegiftshop.entity.CartItem;
import com.utegiftshop.entity.Promotion;
import com.utegiftshop.repository.CartItemRepository;
import com.utegiftshop.repository.PromotionRepository;
import com.utegiftshop.security.service.UserDetailsImpl;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    private UserDetailsImpl getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return null;
        }
        return (UserDetailsImpl) authentication.getPrincipal();
    }

    @GetMapping("/applicable")
    // SỬA: Thay đổi kiểu trả về thành List<PromotionDto>
    public ResponseEntity<List<PromotionDto>> getApplicablePromotions() {
        UserDetailsImpl userDetails = getCurrentUser();
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        List<CartItem> cartItems = cartItemRepository.findByUserId(userDetails.getId());
        if (cartItems.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        Set<Long> shopIdsInCart = cartItems.stream()
                .map(item -> item.getProduct().getShop().getId())
                .collect(Collectors.toSet());

        Timestamp now = new Timestamp(System.currentTimeMillis());

        List<Promotion> adminPromotions = promotionRepository.findByShopIdIsNull();
        List<Promotion> shopPromotions = shopIdsInCart.stream()
                .flatMap(shopId -> promotionRepository.findByShopIdOrderByIdDesc(shopId).stream())
                .collect(Collectors.toList());

        List<Promotion> applicablePromotions = Stream.concat(adminPromotions.stream(), shopPromotions.stream())
                .filter(p -> p.getQuantity() > 0 &&
                             !p.getStartDate().after(now) &&
                             !p.getEndDate().before(now))
                .distinct()
                .collect(Collectors.toList());

        // SỬA: Chuyển đổi danh sách Promotion entity sang PromotionDto trước khi trả về
        List<PromotionDto> promotionDtos = applicablePromotions.stream()
                                                               .map(PromotionDto::new)
                                                               .collect(Collectors.toList());

        return ResponseEntity.ok(promotionDtos);
    }
}
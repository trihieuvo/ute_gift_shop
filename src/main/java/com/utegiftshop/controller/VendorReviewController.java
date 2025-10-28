package com.utegiftshop.controller;

import com.utegiftshop.dto.request.VendorReviewReplyRequestDto;
import com.utegiftshop.dto.response.VendorReviewDto;
import com.utegiftshop.entity.Product;
import com.utegiftshop.entity.Review;
import com.utegiftshop.entity.Shop;
import com.utegiftshop.repository.ProductRepository;
import com.utegiftshop.repository.ReviewRepository;
import com.utegiftshop.repository.ShopRepository;
import com.utegiftshop.security.service.UserDetailsImpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody; // ✅ SỬA: Import đúng từ Spring
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/vendor/reviews")
@PreAuthorize("hasAuthority('Vendor')")
public class VendorReviewController {

    private static final Logger logger = LoggerFactory.getLogger(VendorReviewController.class);

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private ProductRepository productRepository;

    @PersistenceContext
    private EntityManager entityManager;


    private Shop getAuthenticatedShop(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) throw new RuntimeException("Xác thực lỗi.");
        Long userId = userDetails.getId();
        return shopRepository.findByUserId(userId)
                .orElseThrow(() -> {
                     logger.error("Shop Not Found user ID: {}", userId);
                     return new RuntimeException("Cửa hàng chưa được thiết lập.");
                 });
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<?> getShopReviews(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Shop shop = getAuthenticatedShop(userDetails);
            Long shopId = shop.getId();
            logger.info("[getShopReviews] Fetching reviews for Shop ID: {}", shopId);

            String jpql = "SELECT r FROM Review r " +
                          "JOIN FETCH r.user u " +
                          "JOIN FETCH r.product p " +
                          "WHERE p.shop.id = :shopId " +
                          "ORDER BY r.createdAt DESC";

            TypedQuery<Review> query = entityManager.createQuery(jpql, Review.class);
            query.setParameter("shopId", shopId);
            List<Review> reviews = query.getResultList();


            if (reviews.isEmpty()) {
                logger.info("[getShopReviews] No reviews found for Shop ID: {}", shopId);
                return ResponseEntity.ok(Collections.emptyList());
            }

            List<VendorReviewDto> dtos = reviews.stream()
                                                .map(VendorReviewDto::new)
                                                .collect(Collectors.toList());

            logger.info("[getShopReviews] Found {} reviews for Shop ID: {}", dtos.size(), shopId);
            return ResponseEntity.ok(dtos);

        } catch (RuntimeException e) {
            logger.error("[getShopReviews] Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("[getShopReviews] Unexpected error fetching reviews:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(Map.of("message", "Lỗi máy chủ khi tải đánh giá."));
        }
        
    }
    
    @PostMapping(value = "/{reviewId}/reply", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> replyToReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody VendorReviewReplyRequestDto replyRequest, // ✅ Đã sửa import
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Shop shop = getAuthenticatedShop(userDetails);
            Long shopId = shop.getId();
            logger.info("[replyToReview] Vendor (Shop ID: {}) replying to Review ID: {}", shopId, reviewId);

            // Validate request content
            if (replyRequest.getReplyContent() == null || replyRequest.getReplyContent().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .body(Map.of("message", "Nội dung phản hồi không được để trống."));
            }

            // Find the review and ensure it belongs to the vendor's product
            Review review = reviewRepository.findById(reviewId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá (ID: " + reviewId + ")."));

            // Security Check: Verify the product associated with the review belongs to the current vendor's shop
            if (review.getProduct() == null || review.getProduct().getShop() == null || !review.getProduct().getShop().getId().equals(shopId)) {
                logger.warn("[replyToReview] Security violation: Vendor (Shop ID: {}) attempted to reply to review (ID: {}) not belonging to their shop.", shopId, reviewId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .body(Map.of("message", "Bạn không có quyền phản hồi đánh giá này."));
            }

            // Update the review entity
            review.setVendorReply(replyRequest.getReplyContent().trim());
            review.setRepliedAt(new Timestamp(System.currentTimeMillis()));

            Review updatedReview = reviewRepository.save(review);
            logger.info("[replyToReview] Successfully replied to Review ID: {}", reviewId);

            // Return the updated review using the DTO
            return ResponseEntity.ok(new VendorReviewDto(updatedReview));

        } catch (RuntimeException e) {
            logger.error("[replyToReview] Error replying to review ID {}: {}", reviewId, e.getMessage());
             HttpStatus status = e.getMessage().contains("Không tìm thấy đánh giá") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
             return ResponseEntity.status(status)
                                  .contentType(MediaType.APPLICATION_JSON)
                                  .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("[replyToReview] Unexpected error replying to review ID {}:", reviewId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(Map.of("message", "Lỗi máy chủ khi gửi phản hồi."));
        }
    }
}
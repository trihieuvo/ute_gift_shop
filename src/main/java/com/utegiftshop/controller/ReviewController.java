package com.utegiftshop.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utegiftshop.dto.request.ReviewRequestDto;
import com.utegiftshop.dto.response.ReviewDto;
import com.utegiftshop.dto.response.ReviewEligibilityDto;
import com.utegiftshop.entity.Order;
import com.utegiftshop.entity.OrderDetail;
import com.utegiftshop.entity.Product;
import com.utegiftshop.entity.Review;
import com.utegiftshop.entity.User;
import com.utegiftshop.repository.OrderRepository;
import com.utegiftshop.repository.ProductRepository;
import com.utegiftshop.repository.ReviewRepository;
import com.utegiftshop.security.service.UserDetailsImpl;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired private ReviewRepository reviewRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ProductRepository productRepository;

    private UserDetailsImpl getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return null;
        }
        return (UserDetailsImpl) authentication.getPrincipal();
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewDto>> getReviewsForProduct(@PathVariable Long productId) {
        List<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
        List<ReviewDto> dtos = reviews.stream().map(ReviewDto::new).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/eligibility/{productId}")
    public ResponseEntity<ReviewEligibilityDto> checkReviewEligibility(@PathVariable Long productId) {
        UserDetailsImpl userDetails = getCurrentUser();
        if (userDetails == null) {
            return ResponseEntity.ok(new ReviewEligibilityDto(false, null, "Vui lòng đăng nhập để đánh giá."));
        }
        Long userId = userDetails.getId();

        List<Order> deliveredOrders = orderRepository.findByUserId(userId)
                .stream()
                .filter(order -> "DELIVERED".equalsIgnoreCase(order.getStatus()))
                .collect(Collectors.toList());

        for (Order order : deliveredOrders) {
            for (OrderDetail detail : order.getOrderDetails()) {
                if (detail.getProduct().getId().equals(productId)) {
                    boolean alreadyReviewed = reviewRepository.existsByUserIdAndOrderDetailId(userId, detail.getId());
                    if (!alreadyReviewed) {
                        return ResponseEntity.ok(new ReviewEligibilityDto(true, detail.getId(), "Bạn có thể đánh giá sản phẩm này."));
                    }
                }
            }
        }

        return ResponseEntity.ok(new ReviewEligibilityDto(false, null, "Bạn cần mua và nhận hàng thành công để có thể đánh giá sản phẩm này."));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createReview(@Valid @RequestBody ReviewRequestDto request) {
        UserDetailsImpl userDetails = getCurrentUser();
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập."));
        }
        Long userId = userDetails.getId();

        boolean isEligible = orderRepository.findByUserId(userId).stream()
            .filter(order -> "DELIVERED".equalsIgnoreCase(order.getStatus()))
            .flatMap(order -> order.getOrderDetails().stream())
            .anyMatch(detail -> detail.getId().equals(request.getOrderDetailId()) && detail.getProduct().getId().equals(request.getProductId()));

        if (!isEligible) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không đủ điều kiện để đánh giá sản phẩm này."));
        }

        if (reviewRepository.existsByUserIdAndOrderDetailId(userId, request.getOrderDetailId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Bạn đã đánh giá sản phẩm này cho đơn hàng này rồi."));
        }

        Review review = new Review();
        User user = new User();
        user.setId(userId);
        review.setUser(user);

        Product product = new Product();
        product.setId(request.getProductId());
        review.setProduct(product);

        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setId(request.getOrderDetailId());
        review.setOrderDetail(orderDetail);

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        
        Review savedReview = reviewRepository.save(review);
        Review freshReview = reviewRepository.findById(savedReview.getId()).orElse(savedReview);

        return ResponseEntity.status(HttpStatus.CREATED).body(new ReviewDto(freshReview));
    }
}
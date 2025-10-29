package com.utegiftshop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.utegiftshop.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    // Tìm tất cả đánh giá cho một sản phẩm, sắp xếp theo ngày tạo mới nhất
    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

    // Kiểm tra xem người dùng đã đánh giá cho một chi tiết đơn hàng cụ thể chưa (boolean)
    boolean existsByUserIdAndOrderDetailId(Long userId, Long orderDetailId);

    // Tìm đánh giá cụ thể theo user và orderDetail ---
    Optional<Review> findByUserIdAndOrderDetailId(Long userId, Long orderDetailId);
}

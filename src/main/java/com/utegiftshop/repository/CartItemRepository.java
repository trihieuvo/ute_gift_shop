package com.utegiftshop.repository;

import com.utegiftshop.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    // Tìm các sản phẩm trong giỏ hàng của một user
    List<CartItem> findByUserId(Long userId);

    // Tìm một sản phẩm cụ thể trong giỏ hàng của user
    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    // Xóa tất cả sản phẩm trong giỏ của một user (dùng sau khi checkout)
    void deleteByUserId(Long userId);
}
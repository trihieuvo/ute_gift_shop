package com.utegiftshop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository; // <-- BỔ SUNG

import com.utegiftshop.entity.Product; // <-- BỔ SUNG

public interface ProductRepository extends JpaRepository<Product, Long> {
    
    /**
     * BỔ SUNG: Tìm tất cả sản phẩm thuộc về một cửa hàng (Shop)
     */
    List<Product> findByShopId(Long shopId);

    /**
     * BỔ SUNG: Tìm một sản phẩm cụ thể theo ID VÀ ID cửa hàng (dùng để kiểm tra bảo mật)
     */
    Optional<Product> findByIdAndShopId(Long productId, Long shopId);
    List<Product> findByCategoryIdIn(List<Integer> categoryIds);
}
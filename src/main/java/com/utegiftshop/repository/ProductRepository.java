package com.utegiftshop.repository;

import java.math.BigDecimal;
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
    
    // Lọc theo khoảng giá
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // Lọc theo danh mục VÀ khoảng giá
    List<Product> findByCategoryIdInAndPriceBetween(List<Integer> categoryIds, BigDecimal minPrice, BigDecimal maxPrice);
    //Tìm kiếm theo tên sản phẩm (dùng cho chức năng tìm kiếm)
    List<Product> findByNameContainingIgnoreCase(String name);
}
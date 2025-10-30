package com.utegiftshop.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // <-- THÊM IMPORT NÀY
import org.springframework.stereotype.Repository; // <-- THÊM IMPORT NÀY

import com.utegiftshop.entity.Product;

@Repository // <-- THÊM ANNOTATION NÀY
public interface ProductRepository extends JpaRepository<Product, Long> { // <-- SỬA LẠI CHO ĐÚNG

    // --- Giữ nguyên các phương thức cho Vendor ---
    List<Product> findByShopId(Long shopId);
    Optional<Product> findByIdAndShopId(Long productId, Long shopId);

    // === CÁC THAY ĐỔI CHO TRANG HOME (API /api/products) ===

    // 1. Tìm TẤT CẢ sản phẩm ĐANG HOẠT ĐỘNG
    List<Product> findByIsActiveTrue(); // Mới: Chỉ lấy sản phẩm active

    // 2. Tìm theo Danh mục (bao gồm cả con) VÀ ĐANG HOẠT ĐỘNG
    List<Product> findByCategoryIdInAndIsActiveTrue(List<Integer> categoryIds); // Sửa: Thêm AndIsActiveTrue

    // 3. Tìm theo Khoảng giá VÀ ĐANG HOẠT ĐỘNG
    List<Product> findByPriceBetweenAndIsActiveTrue(BigDecimal minPrice, BigDecimal maxPrice); // Sửa: Thêm AndIsActiveTrue

    // 4. Tìm theo Danh mục VÀ Khoảng giá VÀ ĐANG HOẠT ĐỘNG
    List<Product> findByCategoryIdInAndPriceBetweenAndIsActiveTrue(List<Integer> categoryIds, BigDecimal minPrice, BigDecimal maxPrice); // Sửa: Thêm AndIsActiveTrue

    // 5. Tìm kiếm theo Tên VÀ ĐANG HOẠT ĐỘNG
    List<Product> findByNameContainingIgnoreCaseAndIsActiveTrue(String name); // Sửa: Thêm AndIsActiveTrue

    // === BỔ SUNG: Dùng @Query để linh hoạt hơn (Cách khác, có thể thay thế các hàm trên) ===
    // Ví dụ: Lấy tất cả active
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.shop WHERE p.isActive = true")
    List<Product> findAllActiveWithDetails();

    // Ví dụ: Lọc theo categoryId (list), price range và active
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.shop " +
           "WHERE p.isActive = true " +
           "AND (:categoryIds IS NULL OR p.category.id IN :categoryIds) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    List<Product> findActiveProductsFiltered(List<Integer> categoryIds, BigDecimal minPrice, BigDecimal maxPrice);

     // Ví dụ: Tìm kiếm theo keyword và active
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.shop " +
           "WHERE p.isActive = true AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchActiveProductsByName(String keyword);

       @Query("SELECT p FROM Product p JOIN FETCH p.shop s JOIN FETCH s.user")
    List<Product> findAllWithShop();
}
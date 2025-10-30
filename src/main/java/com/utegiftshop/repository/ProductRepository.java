package com.utegiftshop.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository; // <-- THÊM IMPORT NÀY

import com.utegiftshop.entity.Product;
@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    // --- Giữ nguyên các phương thức cho Vendor ---
    List<Product> findByShopId(Long shopId);
    Optional<Product> findByIdAndShopId(Long productId, Long shopId);

    // === CÁC THAY ĐỔI CHO TRANG HOME (API /api/products) ===
    // (Các hàm cũ đã được thay thế bằng các hàm @Query có JOIN FETCH)

    // 1. Tìm TẤT CẢ sản phẩm ĐANG HOẠT ĐỘNG
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.shop s LEFT JOIN FETCH s.user WHERE p.isActive = true")
    List<Product> findByIsActiveTrue();

    // 2. Tìm theo Danh mục (bao gồm cả con) VÀ ĐANG HOẠT ĐỘNG
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.shop s LEFT JOIN FETCH s.user WHERE p.category.id IN :categoryIds AND p.isActive = true")
    List<Product> findByCategoryIdInAndIsActiveTrue(@Param("categoryIds") List<Integer> categoryIds);

    // 3. Tìm theo Khoảng giá VÀ ĐANG HOẠT ĐỘNG
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.shop s LEFT JOIN FETCH s.user WHERE p.price BETWEEN :minPrice AND :maxPrice AND p.isActive = true")
    List<Product> findByPriceBetweenAndIsActiveTrue(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);

    // 3b. (MỚI) Tìm theo giá TỐI THIỂU (cho "Trên 1tr")
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.shop s LEFT JOIN FETCH s.user WHERE p.price >= :minPrice AND p.isActive = true")
    List<Product> findByPriceGreaterThanEqualAndIsActiveTrue(@Param("minPrice") BigDecimal minPrice);

    // 4. Tìm theo Danh mục VÀ Khoảng giá VÀ ĐANG HOẠT ĐỘNG
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.shop s LEFT JOIN FETCH s.user WHERE p.category.id IN :categoryIds AND p.price BETWEEN :minPrice AND :maxPrice AND p.isActive = true")
    List<Product> findByCategoryIdInAndPriceBetweenAndIsActiveTrue(@Param("categoryIds") List<Integer> categoryIds, @Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);

    // 4b. (MỚI) Tìm theo Danh mục VÀ Giá Tối thiểu
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.shop s LEFT JOIN FETCH s.user WHERE p.category.id IN :categoryIds AND p.price >= :minPrice AND p.isActive = true")
    List<Product> findByCategoryIdInAndPriceGreaterThanEqualAndIsActiveTrue(@Param("categoryIds") List<Integer> categoryIds, @Param("minPrice") BigDecimal minPrice);

    // 5. Tìm kiếm theo Tên VÀ ĐANG HOẠT ĐỘNG
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.shop s LEFT JOIN FETCH s.user WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND p.isActive = true")
    List<Product> findByNameContainingIgnoreCaseAndIsActiveTrue(@Param("name") String name);


    // === Dùng cho Admin (Đã sửa từ lần trước) ===
    @Query("SELECT p FROM Product p JOIN FETCH p.shop s JOIN FETCH s.user")
    List<Product> findAllWithShop();

    // === CÁC HÀM QUERY KHÁC (NẾU CÓ) ĐỂ TRỐNG ĐỂ TÙY BIẾN ===
    // (Bỏ các hàm query cũ không có JOIN FETCH)

    // (THÊM HÀM MỚI NÀY)
    // Hàm này cho phép chúng ta dùng JOIN FETCH cùng với Specification
    @Query("SELECT p FROM Product p JOIN FETCH p.shop s JOIN FETCH p.category c")
    List<Product> findAllWithShopAndCategory(Specification<Product> spec);
}
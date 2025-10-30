package com.utegiftshop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.utegiftshop.entity.Promotion;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> { // ID là Integer

    // Tìm tất cả khuyến mãi của một shop, sắp xếp theo ID giảm dần (mới nhất trước)
    List<Promotion> findByShopIdOrderByIdDesc(Long shopId);
    
    // === ADDED: Tìm theo mã code (không phân biệt hoa thường) ===
    List<Promotion> findByShopIdAndCodeContainingIgnoreCaseOrderByIdDesc(Long shopId, String code);
    // === END ADDED ===

    // Tìm một khuyến mãi cụ thể theo ID và shop ID (để kiểm tra quyền sở hữu)
    Optional<Promotion> findByIdAndShopId(Integer promotionId, Long shopId);
    List<Promotion> findByShopIdIsNull();
    
    Optional<Promotion> findByCode(String code);
}
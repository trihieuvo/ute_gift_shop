package com.utegiftshop.repository;

import com.utegiftshop.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> { // ID là Integer

    // Tìm tất cả khuyến mãi của một shop, sắp xếp theo ID giảm dần (mới nhất trước)
    List<Promotion> findByShopIdOrderByIdDesc(Long shopId);

    // Tìm một khuyến mãi cụ thể theo ID và shop ID (để kiểm tra quyền sở hữu)
    Optional<Promotion> findByIdAndShopId(Integer promotionId, Long shopId);
    List<Promotion> findByShopIdIsNull();
}
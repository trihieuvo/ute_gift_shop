package com.utegiftshop.controller;

import com.utegiftshop.entity.Promotion;
import com.utegiftshop.repository.PromotionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/admin/promotions")
public class AdminPromotionApiController {

    private final PromotionRepository promotionRepository;

    public AdminPromotionApiController(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    // API 1: Lấy tất cả Khuyến mãi toàn trang (shop_id = null)
    @GetMapping
    public ResponseEntity<List<Promotion>> getGlobalPromotions() {
        // Tìm tất cả promotions có shop_id là NULL (khuyến mãi toàn trang)
        List<Promotion> promotions = promotionRepository.findByShopIdIsNull(); 
        return ResponseEntity.ok(promotions);
    }

    // API 2: Lưu (Thêm mới/Cập nhật) Khuyến mãi
    @PostMapping
    public ResponseEntity<Promotion> savePromotion(@RequestBody Promotion promotion) {
        // SỬA LỖI Ở ĐÂY: Dùng setShop(null) để đảm bảo đây là khuyến mãi toàn trang
        promotion.setShop(null); 
        
        Promotion savedPromotion = promotionRepository.save(promotion);
        return ResponseEntity.ok(savedPromotion);
    }
    
    // API 3: Xóa Khuyến mãi
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromotion(@PathVariable Integer id) {
        promotionRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
package com.utegiftshop.controller;

import com.utegiftshop.dto.response.ShopDto; 
import com.utegiftshop.entity.Shop;
import com.utegiftshop.repository.ShopRepository;
import com.utegiftshop.security.service.ShopService; 
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/shops")
public class AdminShopApiController {

    private final ShopService shopService;
    private final ShopRepository shopRepository;

    public AdminShopApiController(ShopService shopService, ShopRepository shopRepository) {
        this.shopService = shopService;
        this.shopRepository = shopRepository;
    }

    // API 1: Lấy danh sách Shops (Chung, có thể lọc theo trạng thái)
    @GetMapping("/list")
    public ResponseEntity<List<ShopDto>> getFilteredShops(
        @RequestParam(required = false) String status
    ) {
        List<Shop> shops;
        
        if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("ALL")) {
            // Lọc theo trạng thái cụ thể (PENDING, ACTIVE, REJECTED)
            shops = shopRepository.findByStatus(status); 
        } else {
            // Lấy tất cả shops
            shops = shopRepository.findAll();
        }
        
        // Chuyển đổi sang DTO và trả về
        List<ShopDto> shopDtos = shops.stream()
                                    .map(ShopDto::new)
                                    .collect(Collectors.toList());

        return ResponseEntity.ok(shopDtos);
    }
    
    // API 2: Phê duyệt shop (Giữ nguyên)
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveShop(@PathVariable Long id) {
        shopService.approveShop(id); 
        return ResponseEntity.ok().build();
    }

    // API 3: Từ chối shop (Giữ nguyên)
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectShop(@PathVariable Long id) {
        shopService.rejectShop(id); 
        return ResponseEntity.ok().build();
    }
    
    // API 4: Cập nhật Chiết khấu cho một Shop (CHỨC NĂNG HOA HỒNG)
    @PostMapping("/{id}/commission")
    public ResponseEntity<Void> updateCommission(
            @PathVariable Long id, 
            @RequestParam BigDecimal commissionRate) 
    {
        Optional<Shop> shopOpt = shopRepository.findById(id);

        if (shopOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Shop shop = shopOpt.get();
        
        // Kiểm tra phạm vi 0-100%
        if (commissionRate.compareTo(BigDecimal.ZERO) < 0 || commissionRate.compareTo(new BigDecimal("100")) > 0) {
            return ResponseEntity.badRequest().build();
        }
        
        shop.setCommissionRate(commissionRate);
        shopRepository.save(shop);

        return ResponseEntity.ok().build();
    }
}
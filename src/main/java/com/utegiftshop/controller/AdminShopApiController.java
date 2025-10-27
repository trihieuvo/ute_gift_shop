package com.utegiftshop.controller;

import com.utegiftshop.entity.Shop; 
import com.utegiftshop.security.service.ShopService; // Dùng ShopService của bạn
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/shops") // API được bảo vệ (Bước 1)
public class AdminShopApiController {

    private final ShopService shopService;

    public AdminShopApiController(ShopService shopService) {
        this.shopService = shopService;
    }

    // API lấy danh sách shop "Chờ phê duyệt" 
    @GetMapping("/pending")
    public ResponseEntity<List<Shop>> getPendingShops() {
        List<Shop> pendingShops = shopService.findPendingShops(); 
        return ResponseEntity.ok(pendingShops);
    }

    // API Phê duyệt shop 
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveShop(@PathVariable Long id) {
        shopService.approveShop(id); 
        return ResponseEntity.ok().build();
    }

    // API Từ chối shop 
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectShop(@PathVariable Long id) {
        shopService.rejectShop(id); 
        return ResponseEntity.ok().build();
    }
}
package com.utegiftshop.controller;

import com.utegiftshop.dto.response.ShopDto; // Import DTO
import com.utegiftshop.entity.Shop;
import com.utegiftshop.security.service.ShopService; 
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors; // Import này

@RestController
@RequestMapping("/api/v1/admin/shops")
public class AdminShopApiController {

    private final ShopService shopService;

    public AdminShopApiController(ShopService shopService) {
        this.shopService = shopService;
    }

    // API lấy danh sách shop "Chờ phê duyệt" 
    @GetMapping("/pending")
    public ResponseEntity<List<ShopDto>> getPendingShops() {
        List<Shop> pendingShops = shopService.findPendingShops(); 
        
        // Chuyển đổi List<Shop> sang List<ShopDto>
        // Nó sẽ tự động gọi hàm new ShopDto(shop) mà chúng ta vừa sửa
        List<ShopDto> shopDtos = pendingShops.stream()
                                    .map(ShopDto::new) 
                                    .collect(Collectors.toList());

        return ResponseEntity.ok(shopDtos); // Trả về DTO
    }

    // API Phê duyệt shop (Đã OK)
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveShop(@PathVariable Long id) {
        shopService.approveShop(id); 
        return ResponseEntity.ok().build();
    }

    // API Từ chối shop (Đã OK)
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectShop(@PathVariable Long id) {
        shopService.rejectShop(id); 
        return ResponseEntity.ok().build();
    }
}
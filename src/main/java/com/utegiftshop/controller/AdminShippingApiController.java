package com.utegiftshop.controller;

import com.utegiftshop.entity.ShippingMethod; // Import Entity
import com.utegiftshop.repository.ShippingMethodRepository; // Import Repository
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/admin/shipping-methods")
public class AdminShippingApiController {

    private final ShippingMethodRepository shippingMethodRepository;

    public AdminShippingApiController(ShippingMethodRepository shippingMethodRepository) {
        this.shippingMethodRepository = shippingMethodRepository;
    }

    // API 1: Lấy tất cả đơn vị vận chuyển
    @GetMapping
    public ResponseEntity<List<ShippingMethod>> getAllMethods() {
        // Entity này đơn giản (chỉ có id, name, fee) nên có thể trả về trực tiếp
        List<ShippingMethod> methods = shippingMethodRepository.findAll();
        return ResponseEntity.ok(methods);
    }

    // API 2: Lưu (Thêm mới hoặc Cập nhật)
    // @RequestBody giúp nhận JSON từ JavaScript
    @PostMapping
    public ResponseEntity<ShippingMethod> saveMethod(@RequestBody ShippingMethod method) {
        // Nếu 'id' != null, JPA sẽ tự động hiểu đây là 'update'
        // Nếu 'id' == null, JPA sẽ 'insert'
        ShippingMethod savedMethod = shippingMethodRepository.save(method);
        return ResponseEntity.ok(savedMethod);
    }
    
    // API 3: Xóa
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMethod(@PathVariable Integer id) {
        try {
            shippingMethodRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // Có thể lỗi do đơn hàng đang dùng, v.v.
            return ResponseEntity.badRequest().build();
        }
    }
}
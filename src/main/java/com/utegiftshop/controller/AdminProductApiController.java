package com.utegiftshop.controller;

import com.utegiftshop.entity.Product;
import com.utegiftshop.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/admin/products")
public class AdminProductApiController {

    private final ProductRepository productRepository;

    public AdminProductApiController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // API 1: Lấy tất cả sản phẩm
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        // Trả về trực tiếp Entity vì Product thường không có liên kết vòng lặp phức tạp
        List<Product> products = productRepository.findAll();
        return ResponseEntity.ok(products);
    }

    // API 2: Thay đổi trạng thái (Ẩn/Hiện)
    @PostMapping("/{id}/toggle-active")
    public ResponseEntity<Void> toggleProductStatus(@PathVariable Long id) {
        Optional<Product> productOpt = productRepository.findById(id);
        
        if (productOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Product product = productOpt.get();
        // Lật ngược trạng thái: true -> false (Ẩn), false -> true (Hiện)
        product.setActive(!product.isActive()); 
        productRepository.save(product);

        return ResponseEntity.ok().build();
    }
    // API 3: Xóa sản phẩm vĩnh viễn
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        try {
            productRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // Lỗi khóa ngoại nếu sản phẩm này có trong đơn hàng cũ
            return ResponseEntity.badRequest().build(); 
        }
    }
}


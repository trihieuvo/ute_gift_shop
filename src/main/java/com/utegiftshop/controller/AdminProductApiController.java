package com.utegiftshop.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification; 
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping; 
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam; // (THÊM IMPORT NÀY)
import org.springframework.web.bind.annotation.RestController;

import com.utegiftshop.entity.Product;
import com.utegiftshop.entity.Shop; 
import com.utegiftshop.repository.ProductRepository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate; 

@RestController
@RequestMapping("/api/v1/admin/products")
public class AdminProductApiController {

    private final ProductRepository productRepository;

    public AdminProductApiController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // (SỬA) API 1: Lấy tất cả sản phẩm (Có Lọc)
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String shopName,
            @RequestParam(required = false) Boolean isActive
    ) {
        
        // 1. (SỬA LẠI) Xây dựng Specification
        Specification<Product> spec = (root, query, cb) -> {
            
            // (MỚI) Thêm JOIN FETCH để lấy Shop và Category
            // Chỉ thực hiện fetch khi query là query chính (không phải query count)
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("shop", JoinType.LEFT);
                root.fetch("category", JoinType.LEFT);
            }

            // (MỚI) Cần join() riêng để dùng trong WHERE
            Join<Product, Shop> shopJoin = root.join("shop", JoinType.LEFT);
            
            List<Predicate> predicates = new ArrayList<>();

            // 2. Lọc Tên Sản phẩm
            if (name != null && !name.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }

            // 3. Lọc Tên Shop
            if (shopName != null && !shopName.isEmpty()) {
                predicates.add(cb.like(cb.lower(shopJoin.get("name")), "%" + shopName.toLowerCase() + "%"));
            }

            // 4. Lọc Trạng thái
            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            // (MỚI) Thêm distinct để tránh trùng lặp do fetch
            query.distinct(true);
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 5. (SỬA LẠI) Gọi hàm findAll(spec) CÓ SẴN
        List<Product> products = productRepository.findAll(spec);
        
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
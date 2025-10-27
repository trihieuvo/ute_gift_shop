package com.utegiftshop.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utegiftshop.entity.Product;
import com.utegiftshop.repository.ProductRepository;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        // Lấy tất cả sản phẩm từ database
        List<Product> products = productRepository.findAll();
        // Trả về danh sách sản phẩm với status 200 OK
        return ResponseEntity.ok(products);
    }
}
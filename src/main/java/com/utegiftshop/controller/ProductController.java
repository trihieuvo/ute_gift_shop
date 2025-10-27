package com.utegiftshop.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utegiftshop.dto.request.CategoryDto;
import com.utegiftshop.entity.Category;
import com.utegiftshop.entity.Product;
import com.utegiftshop.repository.CategoryRepository;
import com.utegiftshop.repository.ProductRepository;

@RestController
@RequestMapping("/api") // Thay đổi đường dẫn gốc thành /api
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDto>> getCategories() {
        List<Category> allCategories = categoryRepository.findAll();
        Map<Integer, CategoryDto> dtoMap = allCategories.stream()
                .collect(Collectors.toMap(Category::getId, CategoryDto::new));

        List<CategoryDto> rootCategories = new ArrayList<>();

        allCategories.forEach(category -> {
            CategoryDto currentDto = dtoMap.get(category.getId());
            if (category.getParent() != null) {
                CategoryDto parentDto = dtoMap.get(category.getParent().getId());
                if (parentDto != null) {
                    parentDto.getChildren().add(currentDto);
                }
            } else {
                rootCategories.add(currentDto);
            }
        });

        return ResponseEntity.ok(rootCategories);
    }
}
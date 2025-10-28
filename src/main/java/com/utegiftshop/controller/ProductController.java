package com.utegiftshop.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.utegiftshop.dto.request.CategoryDto;
import com.utegiftshop.entity.Category;
import com.utegiftshop.entity.Product;
import com.utegiftshop.repository.CategoryRepository;
import com.utegiftshop.repository.ProductRepository;

@RestController
@RequestMapping("/api")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // Helper method to find all sub-category IDs recursively
    private void getAllSubCategoryIds(Integer parentId, Map<Integer, List<Category>> parentToChildrenMap, Set<Integer> allIds) {
        allIds.add(parentId);
        List<Category> children = parentToChildrenMap.get(parentId);
        if (children != null) {
            for (Category child : children) {
                getAllSubCategoryIds(child.getId(), parentToChildrenMap, allIds);
            }
        }
    }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {

        List<Product> products;
        if (keyword != null && !keyword.isEmpty()) {
            products = productRepository.findByNameContainingIgnoreCase(keyword);
            return ResponseEntity.ok(products);
        }
        boolean hasCategoryFilter = categoryId != null;
        boolean hasPriceFilter = minPrice != null && maxPrice != null;

        if (hasCategoryFilter) {
            // Lấy ID của category và các category con
            List<Category> allCategories = categoryRepository.findAll();
            Map<Integer, List<Category>> parentToChildrenMap = allCategories.stream()
                    .filter(c -> c.getParent() != null)
                    .collect(Collectors.groupingBy(c -> c.getParent().getId()));

            Set<Integer> categoryIdsToSearch = new HashSet<>();
            getAllSubCategoryIds(categoryId, parentToChildrenMap, categoryIdsToSearch);
            List<Integer> categoryIdList = new ArrayList<>(categoryIdsToSearch);

            if (hasPriceFilter) {
                // Lọc theo cả danh mục và giá
                products = productRepository.findByCategoryIdInAndPriceBetween(categoryIdList, minPrice, maxPrice);
            } else {
                // Chỉ lọc theo danh mục
                products = productRepository.findByCategoryIdIn(categoryIdList);
            }
        } else {
            if (hasPriceFilter) {
                // Chỉ lọc theo giá
                products = productRepository.findByPriceBetween(minPrice, maxPrice);
            } else {
                // Không có bộ lọc nào
                products = productRepository.findAll();
            }
        }
        return ResponseEntity.ok(products);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());

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
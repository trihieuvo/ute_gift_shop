package com.utegiftshop.controller;

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
    public ResponseEntity<List<Product>> getAllProducts(@RequestParam(required = false) Integer categoryId) {
        if (categoryId == null) {
            // No filter, return all products
            List<Product> products = productRepository.findAll();
            return ResponseEntity.ok(products);
        } else {
            // Filter by category and its children
            List<Category> allCategories = categoryRepository.findAll();
            Map<Integer, List<Category>> parentToChildrenMap = allCategories.stream()
                    .filter(c -> c.getParent() != null)
                    .collect(Collectors.groupingBy(c -> c.getParent().getId()));

            Set<Integer> categoryIdsToSearch = new HashSet<>();
            getAllSubCategoryIds(categoryId, parentToChildrenMap, categoryIdsToSearch);

            List<Product> filteredProducts = productRepository.findByCategoryIdIn(new ArrayList<>(categoryIdsToSearch));
            return ResponseEntity.ok(filteredProducts);
        }
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
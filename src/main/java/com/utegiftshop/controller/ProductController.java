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

    // Helper method giữ nguyên
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

        // Ưu tiên tìm kiếm theo keyword trước
        if (keyword != null && !keyword.isEmpty()) {
            // *** SỬA: Dùng phương thức tìm kiếm chỉ sản phẩm active ***
            products = productRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(keyword);
            return ResponseEntity.ok(products);
        }

        boolean hasCategoryFilter = categoryId != null;
        // Sửa: Kiểm tra cả minPrice và maxPrice riêng lẻ hoặc cùng nhau tùy logic bạn muốn
        // Ở đây, giả sử cần cả hai để lọc khoảng giá
        boolean hasPriceFilter = (minPrice != null && maxPrice != null && minPrice.compareTo(BigDecimal.ZERO) >= 0 && maxPrice.compareTo(minPrice) >= 0);
        // Hoặc nếu muốn lọc chỉ min hoặc chỉ max:
        // boolean hasPriceFilter = (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) >= 0) || (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) >= 0);

        List<Integer> categoryIdList = null;
        if (hasCategoryFilter) {
            // Logic lấy category con giữ nguyên
            List<Category> allCategories = categoryRepository.findAll();
            Map<Integer, List<Category>> parentToChildrenMap = allCategories.stream()
                    .filter(c -> c.getParent() != null)
                    .collect(Collectors.groupingBy(c -> c.getParent().getId()));
            Set<Integer> categoryIdsToSearch = new HashSet<>();
            getAllSubCategoryIds(categoryId, parentToChildrenMap, categoryIdsToSearch);
            categoryIdList = new ArrayList<>(categoryIdsToSearch);
        }

        // *** SỬA LOGIC LỌC ***
        if (hasCategoryFilter && hasPriceFilter) {
            // Lọc theo cả danh mục (và con) VÀ giá VÀ active
            products = productRepository.findByCategoryIdInAndPriceBetweenAndIsActiveTrue(categoryIdList, minPrice, maxPrice);
        } else if (hasCategoryFilter) {
            // Chỉ lọc theo danh mục (và con) VÀ active
            products = productRepository.findByCategoryIdInAndIsActiveTrue(categoryIdList);
        } else if (hasPriceFilter) {
            // Chỉ lọc theo giá VÀ active
            products = productRepository.findByPriceBetweenAndIsActiveTrue(minPrice, maxPrice);
        } else {
            // Không có bộ lọc nào -> Lấy tất cả sản phẩm active
            products = productRepository.findByIsActiveTrue();
        }

        return ResponseEntity.ok(products);
    }

    // Phương thức getProductById và getCategories giữ nguyên
    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        // *** SỬA: Chỉ trả về nếu sản phẩm active ***
        return productRepository.findById(id)
                .filter(Product::isActive) // Thêm bộ lọc ở đây
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDto>> getCategories() {
       // ... (Giữ nguyên logic)
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
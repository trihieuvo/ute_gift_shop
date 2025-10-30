package com.utegiftshop.controller;

 import java.math.BigDecimal;
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Optional;
 import java.util.Set;
 import java.util.stream.Collectors;

 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.http.ResponseEntity;
 import org.springframework.transaction.annotation.Transactional;
 import org.springframework.web.bind.annotation.GetMapping;
 import org.springframework.web.bind.annotation.PathVariable;
 import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.bind.annotation.RequestParam;
 import org.springframework.web.bind.annotation.RestController;
 import com.utegiftshop.dto.response.ProductDetailDto;
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

     // ========================================================================
     // API LẤY DANH SÁCH SẢN PHẨM (getAllProducts) - ĐÃ SỬA LỖI LOGIC LỌC
     // ========================================================================
     
     // Helper method (giữ nguyên)
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

         // 1. Ưu tiên tìm kiếm theo từ khóa (nếu có)
         if (keyword != null && !keyword.isEmpty()) {
             products = productRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(keyword);
             return ResponseEntity.ok(products);
         }

         // 2. Lọc theo danh mục và giá
         boolean hasCategoryFilter = categoryId != null;
         
         // SỬA LẠI LOGIC KIỂM TRA GIÁ
         boolean hasMinPrice = (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) >= 0);
         // (maxPrice có thể là null nếu người dùng chọn "Trên 1tr")
         boolean hasMaxPrice = (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) >= 0 && (!hasMinPrice || maxPrice.compareTo(minPrice) >= 0));

         List<Integer> categoryIdList = null;
         if (hasCategoryFilter) {
             List<Category> allCategories = categoryRepository.findAll();
             Map<Integer, List<Category>> parentToChildrenMap = allCategories.stream()
                     .filter(c -> c.getParent() != null)
                     .collect(Collectors.groupingBy(c -> c.getParent().getId()));
             Set<Integer> categoryIdsToSearch = new HashSet<>();
             getAllSubCategoryIds(categoryId, parentToChildrenMap, categoryIdsToSearch);
             categoryIdList = new ArrayList<>(categoryIdsToSearch);
         }

         // --- Bắt đầu logic lọc MỚI và ĐÚNG ---
         if (hasCategoryFilter) {
             if (hasMinPrice && hasMaxPrice) {
                 // Lọc theo Category, MinPrice, MaxPrice
                 products = productRepository.findByCategoryIdInAndPriceBetweenAndIsActiveTrue(categoryIdList, minPrice, maxPrice);
             } else if (hasMinPrice) {
                 // Lọc theo Category và MinPrice (trường hợp "Trên 1.000.000đ")
                 products = productRepository.findByCategoryIdInAndPriceGreaterThanEqualAndIsActiveTrue(categoryIdList, minPrice);
             } else { // (hasMaxPrice không thể xảy ra một mình theo logic JS)
                 // Chỉ lọc theo Category
                 products = productRepository.findByCategoryIdInAndIsActiveTrue(categoryIdList);
             }
         } else {
             // Không lọc theo Category
             if (hasMinPrice && hasMaxPrice) {
                 // Lọc theo MinPrice, MaxPrice
                 products = productRepository.findByPriceBetweenAndIsActiveTrue(minPrice, maxPrice);
             } else if (hasMinPrice) {
                 // Chỉ lọc theo MinPrice (trường hợp "Trên 1.000.000đ")
                 products = productRepository.findByPriceGreaterThanEqualAndIsActiveTrue(minPrice);
             } else {
                 // Không có bất kỳ bộ lọc giá/category nào
                 products = productRepository.findByIsActiveTrue();
             }
         }
         // --- Kết thúc logic lọc mới ---

         return ResponseEntity.ok(products);
     }


     @GetMapping("/products/{id}")
     @Transactional(readOnly = true)
     public ResponseEntity<?> getProductById(@PathVariable Long id) {
         Optional<Product> productOpt = productRepository.findById(id);

         if (productOpt.isPresent()) {
             Product product = productOpt.get();
             if (!product.isActive()) {
                 return ResponseEntity.notFound().build();
             }
             ProductDetailDto dto = new ProductDetailDto(product);
             return ResponseEntity.ok(dto);
         } else {
             return ResponseEntity.notFound().build();
         }
     }


     // ========================================================================
     // API LẤY DANH MỤC (getCategories) - Giữ nguyên
     // ========================================================================
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
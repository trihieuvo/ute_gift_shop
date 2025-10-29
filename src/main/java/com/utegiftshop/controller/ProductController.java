package com.utegiftshop.controller;

 import java.math.BigDecimal;
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Optional; // Đảm bảo có import Optional
 import java.util.Set;
 import java.util.stream.Collectors;

 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.http.ResponseEntity;
 import org.springframework.transaction.annotation.Transactional; // ✅ Thêm import này
 import org.springframework.web.bind.annotation.GetMapping;
 import org.springframework.web.bind.annotation.PathVariable;
 import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.bind.annotation.RequestParam;
 import org.springframework.web.bind.annotation.RestController;
import com.utegiftshop.dto.response.ProductDetailDto;
 import com.utegiftshop.dto.request.CategoryDto; // Giữ lại nếu bạn có fetch category
 import com.utegiftshop.entity.Category;
 import com.utegiftshop.entity.Product;
 import com.utegiftshop.repository.CategoryRepository;
 import com.utegiftshop.repository.ProductRepository;
 // Giả sử bạn không cần các import liên quan đến lọc sản phẩm ở đây nữa

 @RestController
 @RequestMapping("/api")
 public class ProductController {

     @Autowired
     private ProductRepository productRepository;

     @Autowired
     private CategoryRepository categoryRepository; // Giữ lại nếu cần

     // ========================================================================
     // API LẤY DANH SÁCH SẢN PHẨM (getAllProducts) - Giữ nguyên logic lọc cũ
     // ========================================================================
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

         if (keyword != null && !keyword.isEmpty()) {
             products = productRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(keyword);
             return ResponseEntity.ok(products);
         }

         boolean hasCategoryFilter = categoryId != null;
         boolean hasPriceFilter = (minPrice != null && maxPrice != null && minPrice.compareTo(BigDecimal.ZERO) >= 0 && maxPrice.compareTo(minPrice) >= 0);

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

         if (hasCategoryFilter && hasPriceFilter) {
             products = productRepository.findByCategoryIdInAndPriceBetweenAndIsActiveTrue(categoryIdList, minPrice, maxPrice);
         } else if (hasCategoryFilter) {
             products = productRepository.findByCategoryIdInAndIsActiveTrue(categoryIdList);
         } else if (hasPriceFilter) {
             products = productRepository.findByPriceBetweenAndIsActiveTrue(minPrice, maxPrice);
         } else {
             products = productRepository.findByIsActiveTrue();
         }

         return ResponseEntity.ok(products);
     }


     @GetMapping("/products/{id}")
     @Transactional(readOnly = true)
     public ResponseEntity<?> getProductById(@PathVariable Long id) { // <<< Sửa thành ResponseEntity<?>
         Optional<Product> productOpt = productRepository.findById(id);

         if (productOpt.isPresent()) {
             Product product = productOpt.get();

             // Chỉ trả về nếu sản phẩm đang active
             if (!product.isActive()) {
                 System.out.println("Product ID " + id + " is not active. Returning 404.");
                 return ResponseEntity.notFound().build();
             }

             // Tạo và trả về DTO thay vì Entity
             ProductDetailDto dto = new ProductDetailDto(product); // <<< TẠO DTO
             System.out.println("Returning product detail DTO for ID " + id);
             return ResponseEntity.ok(dto); // <<< TRẢ VỀ DTO
         } else {
             System.out.println("Product ID " + id + " not found in repository. Returning 404.");
             return ResponseEntity.notFound().build();
         }
     }


     // ========================================================================
     // API LẤY DANH MỤC (getCategories) - Giữ nguyên
     // ========================================================================
     @GetMapping("/categories")
     public ResponseEntity<List<CategoryDto>> getCategories() {
        List<Category> allCategories = categoryRepository.findAll();
        // Logic tạo cây danh mục (giữ nguyên từ file bạn cung cấp)
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
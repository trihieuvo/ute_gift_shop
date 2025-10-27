package com.utegiftshop.controller;

import com.utegiftshop.dto.request.CategoryDto;
import com.utegiftshop.dto.request.ProductVendorRequestDto;
import com.utegiftshop.dto.response.ProductVendorDto;
import com.utegiftshop.entity.Category;
import com.utegiftshop.entity.Product;
import com.utegiftshop.entity.Shop;
import com.utegiftshop.repository.CategoryRepository;
import com.utegiftshop.repository.ProductRepository;
import com.utegiftshop.repository.ShopRepository;
import com.utegiftshop.security.service.UserDetailsImpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vendor")
@PreAuthorize("hasAuthority('Vendor')")
public class VendorApiController {

    private static final Logger logger = LoggerFactory.getLogger(VendorApiController.class);

    @Autowired private ProductRepository productRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private CategoryRepository categoryRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // --- Helper lấy Shop ---
    private Shop getAuthenticatedShop() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return shopRepository.findByUserId(userDetails.getId())
                .orElseThrow(() -> {
                     logger.error("Vendor Shop Not Found for user ID: {}", userDetails.getId());
                     return new RuntimeException("Cửa hàng của bạn chưa được thiết lập hoặc không tìm thấy.");
                 });
    }

    // =============================================
    // API LẤY DANH SÁCH SẢN PHẨM (Trả về List<ProductVendorDto>)
    // =============================================
    @GetMapping(value = "/products", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMyProducts() {
        try {
            Shop shop = getAuthenticatedShop();
            logger.info("Fetching products for Shop ID: {}", shop.getId());
            String jpql = "SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.shop.id = :shopId ORDER BY p.createdAt DESC";
            TypedQuery<Product> query = entityManager.createQuery(jpql, Product.class);
            query.setParameter("shopId", shop.getId());
            List<Product> products = query.getResultList();
            logger.info("Found {} products for Shop ID: {}", products.size(), shop.getId());
            List<ProductVendorDto> dtos = products.stream().map(ProductVendorDto::new).collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (RuntimeException e) { /* ... return 404 ... */ logger.error("Error getting vendor products: {}", e.getMessage()); return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body("{\"message\": \"" + e.getMessage() + "\"}"); }
          catch (Exception e) { /* ... return 500 ... */ logger.error("Unexpected error fetching vendor products:", e); return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body("{\"message\": \"Lỗi máy chủ khi tải sản phẩm.\"}"); }
    }

    // =============================================
    // API LẤY CHI TIẾT 1 SẢN PHẨM (Trả về Product Entity - CÓ JOIN FETCH)
    // =============================================
    @GetMapping(value = "/products/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true) // Cần Transactional để FETCH hoạt động
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
         try {
            Shop shop = getAuthenticatedShop();
            logger.info("Fetching product detail ID: {} for Shop ID: {}", id, shop.getId());

            // !!!!! ĐẢM BẢO CÓ LEFT JOIN FETCH p.category !!!!!
            String jpql = "SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.id = :productId AND p.shop.id = :shopId";
            TypedQuery<Product> query = entityManager.createQuery(jpql, Product.class);
            query.setParameter("productId", id);
            query.setParameter("shopId", shop.getId());
            Product product = query.getResultStream().findFirst().orElse(null);

            if (product == null) {
                 logger.warn("Product ID: {} not found or does not belong to Shop ID: {}", id, shop.getId());
                 return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body("{\"message\":\"Không tìm thấy sản phẩm hoặc sản phẩm không thuộc cửa hàng của bạn.\"}");
            }
            logger.info("Product detail found: {}", product.getName());
            // Log category để kiểm tra
            if (product.getCategory() != null) logger.info("Product Category ID: {}, Name: {}", product.getCategory().getId(), product.getCategory().getName());
            else logger.warn("Product ID: {} has NULL category.", id);

            return ResponseEntity.ok(product); // Trả về Entity

        } catch (RuntimeException e) { /* ... return 404/400 ... */ logger.error("Error getting product detail: {}", e.getMessage()); return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body("{\"message\": \"" + e.getMessage() + "\"}"); }
          catch (Exception e) { /* ... return 500 ... */ logger.error("Unexpected error fetching product detail ID: {}", id, e); return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body("{\"message\": \"Lỗi máy chủ khi tải chi tiết sản phẩm.\"}"); }
    }

    // =============================================
    // API THÊM SẢN PHẨM MỚI (Trả về ProductVendorDto)
    // =============================================
    @PostMapping(value = "/products", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> createProduct(@RequestBody ProductVendorRequestDto request) {
        // ... (Giữ nguyên logic như trước, đảm bảo bắt lỗi RuntimeException) ...
        try {
            Shop shop = getAuthenticatedShop();
            logger.info("Creating new product for Shop ID: {} with name: {}", shop.getId(), request.getName());
            Category category = categoryRepository.findById(request.getCategoryId()).orElseThrow(() -> new RuntimeException("Danh mục không hợp lệ (ID: " + request.getCategoryId() + ")"));
            Product product = new Product();
            product.setShop(shop); product.setCategory(category); product.setName(request.getName()); product.setDescription(request.getDescription()); product.setPrice(request.getPrice()); product.setStockQuantity(request.getStockQuantity());
            // Lưu ý: imageUrl chỉ nên là tên file nếu dùng ImageController, hoặc URL đầy đủ nếu là link ngoài
            product.setImageUrl(request.getImageUrl());
            product.setActive(request.isActive()); Timestamp now = new Timestamp(System.currentTimeMillis()); product.setCreatedAt(now); product.setUpdatedAt(now);
            Product savedProduct = productRepository.save(product);
            logger.info("Product created successfully with ID: {}", savedProduct.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(new ProductVendorDto(savedProduct));
        } catch (RuntimeException e) { /* ... return 400 ... */ logger.error("Error creating product: {}", e.getMessage()); return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body("{\"message\": \"" + e.getMessage() + "\"}"); }
          catch (Exception e) { /* ... return 500 ... */ logger.error("Unexpected error creating product:", e); return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body("{\"message\": \"Lỗi máy chủ khi thêm sản phẩm.\"}"); }
    }

    // =============================================
    // API CẬP NHẬT SẢN PHẨM (Trả về ProductVendorDto)
    // =============================================
    @PutMapping(value = "/products/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody ProductVendorRequestDto request) {
         // ... (Giữ nguyên logic như trước, đảm bảo bắt lỗi RuntimeException) ...
         try {
            Shop shop = getAuthenticatedShop(); logger.info("Updating product ID: {} for Shop ID: {}", id, shop.getId());
            Product product = productRepository.findByIdAndShopId(id, shop.getId()).orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm hoặc sản phẩm không thuộc cửa hàng của bạn (ID: " + id + ")"));
            Category category = categoryRepository.findById(request.getCategoryId()).orElseThrow(() -> new RuntimeException("Danh mục không hợp lệ (ID: " + request.getCategoryId() + ")"));
            product.setCategory(category); product.setName(request.getName()); product.setDescription(request.getDescription()); product.setPrice(request.getPrice()); product.setStockQuantity(request.getStockQuantity());
            // Lưu ý: imageUrl chỉ nên là tên file nếu dùng ImageController
            product.setImageUrl(request.getImageUrl());
            product.setActive(request.isActive()); product.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            Product updatedProduct = productRepository.save(product); logger.info("Product ID: {} updated successfully.", id);
            return ResponseEntity.ok(new ProductVendorDto(updatedProduct));
         } catch (RuntimeException e) { /* ... return 404/400 ... */ logger.error("Error updating product ID: {}: {}", id, e.getMessage()); HttpStatus status = e.getMessage().contains("Không tìm thấy sản phẩm") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST; return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body("{\"message\": \"" + e.getMessage() + "\"}"); }
           catch (Exception e) { /* ... return 500 ... */ logger.error("Unexpected error updating product ID: {}", id, e); return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body("{\"message\": \"Lỗi máy chủ khi cập nhật sản phẩm.\"}"); }
    }

    // =============================================
    // API LẤY DANH MỤC (Trả về List<CategoryDto>)
    // =============================================
    @GetMapping(value = "/categories", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CategoryDto>> getAllCategories() {
        // ... (Giữ nguyên logic như trước) ...
        try { logger.info("Fetching all categories."); List<CategoryDto> dtos = categoryRepository.findAll().stream().map(CategoryDto::new).collect(Collectors.toList()); logger.info("Found {} categories.", dtos.size()); return ResponseEntity.ok(dtos); } catch (Exception e) { logger.error("Error fetching categories:", e); return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList()); }
    }
}
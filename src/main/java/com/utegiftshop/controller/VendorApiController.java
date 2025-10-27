	package com.utegiftshop.controller;
	
	import java.sql.Timestamp;
	import java.util.ArrayList;
	import java.util.Collections;
	import java.util.List;
	import java.util.stream.Collectors;

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
	import org.springframework.web.bind.annotation.GetMapping;
	import org.springframework.web.bind.annotation.PathVariable;
	import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
	import org.springframework.web.bind.annotation.RequestMapping;
	import org.springframework.web.bind.annotation.RestController;

	import com.utegiftshop.dto.request.CategoryDto;
	import com.utegiftshop.dto.request.ProductVendorRequestDto;
	import com.utegiftshop.dto.response.ProductVendorDto;
	import com.utegiftshop.entity.Category;
	import com.utegiftshop.entity.Product;
import com.utegiftshop.entity.ProductImage;
import com.utegiftshop.entity.Shop;
	import com.utegiftshop.repository.CategoryRepository;
	import com.utegiftshop.repository.ProductRepository;
import com.utegiftshop.repository.ShopRepository;
import com.utegiftshop.security.service.UserDetailsImpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.web.bind.annotation.DeleteMapping; // Thêm import này
import org.springframework.dao.DataIntegrityViolationException; // Thêm import này để xử lý ràng buộc
import java.util.Map;
	
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
			try {
				Shop shop = getAuthenticatedShop();
				logger.info("Creating new product for Shop ID: {} with name: {}", shop.getId(), request.getName());
				Category category = categoryRepository.findById(request.getCategoryId()).orElseThrow(() -> new RuntimeException("Danh mục không hợp lệ (ID: " + request.getCategoryId() + ")"));
				Product product = new Product();
				product.setShop(shop); 
				product.setCategory(category); 
				product.setName(request.getName()); 
				product.setDescription(request.getDescription()); 
				product.setPrice(request.getPrice()); 
				product.setStockQuantity(request.getStockQuantity());
				product.setActive(request.isActive()); 
				Timestamp now = new Timestamp(System.currentTimeMillis()); 
				product.setCreatedAt(now); 
				product.setUpdatedAt(now);

				// === THAY ĐỔI LOGIC LƯU ẢNH ===
				if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
					List<ProductImage> images = new ArrayList<>();
					for (String url : request.getImageUrls()) {
						ProductImage productImage = new ProductImage();
						productImage.setImageUrl(url);
						productImage.setProduct(product); // Liên kết ảnh với sản phẩm
						images.add(productImage);
					}
					product.setImages(images);
				}
				// === KẾT THÚC THAY ĐỔI ===

				Product savedProduct = productRepository.save(product);
				logger.info("Product created successfully with ID: {}", savedProduct.getId());
				return ResponseEntity.status(HttpStatus.CREATED).body(new ProductVendorDto(savedProduct));
			} catch (RuntimeException e) {
				logger.error("Error creating product: {}", e.getMessage()); 
				return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body("{\"message\": \"" + e.getMessage() + "\"}"); 
			} catch (Exception e) {
				logger.error("Unexpected error creating product:", e); 
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body("{\"message\": \"Lỗi máy chủ khi thêm sản phẩm.\"}"); 
			}
		}
	
	    // =============================================
	    // API CẬP NHẬT SẢN PHẨM (Trả về ProductVendorDto)
	    // =============================================
	    @PutMapping(value = "/products/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
		@Transactional
		public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody ProductVendorRequestDto request) {
			try {
				Shop shop = getAuthenticatedShop(); 
				logger.info("Updating product ID: {} for Shop ID: {}", id, shop.getId());
				Product product = productRepository.findByIdAndShopId(id, shop.getId()).orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm hoặc sản phẩm không thuộc cửa hàng của bạn (ID: " + id + ")"));
				Category category = categoryRepository.findById(request.getCategoryId()).orElseThrow(() -> new RuntimeException("Danh mục không hợp lệ (ID: " + request.getCategoryId() + ")"));
				product.setCategory(category); 
				product.setName(request.getName()); 
				product.setDescription(request.getDescription()); 
				product.setPrice(request.getPrice()); 
				product.setStockQuantity(request.getStockQuantity());
				product.setActive(request.isActive()); 
				product.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

				// === THAY ĐỔI LOGIC CẬP NHẬT ẢNH ===
				// Xóa ảnh cũ và thêm ảnh mới
				if (product.getImages() != null) {
					product.getImages().clear();
				} else {
					product.setImages(new ArrayList<>());
				}

				if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
					for (String url : request.getImageUrls()) {
						ProductImage productImage = new ProductImage();
						productImage.setImageUrl(url);
						productImage.setProduct(product);
						product.getImages().add(productImage);
					}
				}
				// === KẾT THÚC THAY ĐỔI ===

				Product updatedProduct = productRepository.save(product); 
				logger.info("Product ID: {} updated successfully.", id);
				return ResponseEntity.ok(new ProductVendorDto(updatedProduct));
			} catch (RuntimeException e) {
				logger.error("Error updating product ID: {}: {}", id, e.getMessage()); 
				HttpStatus status = e.getMessage().contains("Không tìm thấy sản phẩm") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST; 
				return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body("{\"message\": \"" + e.getMessage() + "\"}"); 
			} catch (Exception e) {
				logger.error("Unexpected error updating product ID: {}", id, e); 
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body("{\"message\": \"Lỗi máy chủ khi cập nhật sản phẩm.\"}"); 
			}
		}
	
	    // =============================================
	    // API LẤY DANH MỤC (Trả về List<CategoryDto>)
	    // =============================================
	    @GetMapping(value = "/categories", produces = MediaType.APPLICATION_JSON_VALUE)
	    public ResponseEntity<List<CategoryDto>> getAllCategories() {
	        // ... (Giữ nguyên logic như trước) ...
	        try { logger.info("Fetching all categories."); List<CategoryDto> dtos = categoryRepository.findAll().stream().map(CategoryDto::new).collect(Collectors.toList()); logger.info("Found {} categories.", dtos.size()); return ResponseEntity.ok(dtos); } catch (Exception e) { logger.error("Error fetching categories:", e); return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList()); }
	    }
	    @DeleteMapping(value = "/products/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	    @Transactional
	    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
	        try {
	            Shop shop = getAuthenticatedShop(); // Lấy thông tin shop của vendor đang đăng nhập
	            Long shopId = shop.getId();
	            logger.info("🗑️ Đang xóa sản phẩm ID: {} cho Shop ID: {}", id, shopId);

	            // 1. Tìm sản phẩm theo ID và Shop ID để đảm bảo quyền sở hữu
	            Product product = productRepository.findByIdAndShopId(id, shopId)
	                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm (ID: " + id + ") hoặc sản phẩm không thuộc cửa hàng của bạn."));

	            // 2. Thực hiện xóa
	            productRepository.delete(product);

	            logger.info("✅ Sản phẩm ID: {} đã được xóa thành công.", id);

	            // 3. Trả về thông báo thành công
	            return ResponseEntity.ok(Map.of("message", "Đã xóa sản phẩm '" + product.getName() + "' thành công."));

	        } catch (DataIntegrityViolationException e) { // <-- ĐƯA LÊN TRƯỚC
	            logger.error("❌ Vi phạm ràng buộc khi xóa sản phẩm ID {}: {}", id, e.getMessage());
	            // Trả về lỗi 409 Conflict
	            return ResponseEntity.status(HttpStatus.CONFLICT)
	                                 .contentType(MediaType.APPLICATION_JSON)
	                                 .body(Map.of("message", "Không thể xóa sản phẩm này vì nó đã được tham chiếu (ví dụ: trong đơn hàng đã đặt). Vui lòng ẩn sản phẩm thay vì xóa."));
	        } catch (RuntimeException e) { // <-- ĐƯA XUỐNG SAU
	            logger.error("❌ Lỗi khi xóa sản phẩm ID {}: {}", id, e.getMessage());
	            // Trả về lỗi 404 Not Found nếu là lỗi không tìm thấy, nếu không thì lỗi khác
	             HttpStatus status = e.getMessage().contains("Không tìm thấy sản phẩm") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST; // Hoặc một status khác phù hợp
	             return ResponseEntity.status(status)
	                                  .contentType(MediaType.APPLICATION_JSON)
	                                  .body(Map.of("message", e.getMessage()));
	        } catch (Exception e) { // Bắt các lỗi không mong muốn khác
	            logger.error("❌ Lỗi không mong muốn khi xóa sản phẩm ID {}:", id, e);
	            // Trả về lỗi 500 Internal Server Error
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                                 .contentType(MediaType.APPLICATION_JSON)
	                                 .body(Map.of("message", "Lỗi máy chủ không mong muốn khi xóa sản phẩm."));
	        }
	    }
	    
	    
	    
	}
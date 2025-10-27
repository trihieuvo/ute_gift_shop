package com.utegiftshop.controller;

import com.utegiftshop.dto.request.CategoryRequest;
import com.utegiftshop.entity.Category;
import com.utegiftshop.security.service.CategoryService; // Đảm bảo import đúng
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException; // Import thêm

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/categories") // API được bảo vệ bởi SecurityConfig
public class AdminCategoryApiController {

    private final CategoryService categoryService;

    public AdminCategoryApiController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // API lấy danh sách (Dùng chung API public /api/categories đã tạo trước đó)
    // Bạn không cần tạo lại hàm GET all ở đây. Frontend sẽ gọi /api/categories.

    // API lấy chi tiết 1 category (để sửa)
    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Integer id) {
        return categoryService.findCategoryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // API tạo category mới (UC-007)
    @PostMapping
    public ResponseEntity<Category> createCategory(@Valid @RequestBody CategoryRequest request) {
        try {
            Category newCategory = categoryService.createCategory(request);
            return new ResponseEntity<>(newCategory, HttpStatus.CREATED);
        } catch (Exception e) {
             // Trả về lỗi nếu có vấn đề (ví dụ: tên trùng lặp nếu bạn cài unique constraint)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    // API cập nhật category (UC-007)
    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Integer id, @Valid @RequestBody CategoryRequest request) {
        try {
            Category updatedCategory = categoryService.updateCategory(id, request);
            return ResponseEntity.ok(updatedCategory);
        } catch (IllegalArgumentException e) {
             // Trả về 404 nếu không tìm thấy ID
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
             // Các lỗi khác
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    // API xóa category (UC-007)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Integer id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.noContent().build(); // Trả về 204 No Content khi thành công
        } catch (Exception e) { // Bắt lỗi chung (ví dụ: không cho xóa nếu có sản phẩm)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
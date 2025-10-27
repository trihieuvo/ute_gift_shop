package com.utegiftshop.security.service;

import com.utegiftshop.dto.request.CategoryRequest; // Đảm bảo DTO tên là CategoryRequest
import com.utegiftshop.entity.Category;
import com.utegiftshop.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    // (Nếu cần kiểm tra sản phẩm trước khi xóa, thêm ProductRepository)
    // private final ProductRepository productRepository; 

    public CategoryService(CategoryRepository categoryRepository /*, ProductRepository productRepository */) {
        this.categoryRepository = categoryRepository;
        // this.productRepository = productRepository;
    }

    // (Dùng chung) Lấy tất cả danh mục
    @Transactional(readOnly = true)
    public List<Category> findAllCategories() {
        return categoryRepository.findAll();
    }

    // (Admin) Lấy chi tiết 1 category
    @Transactional(readOnly = true)
    public Optional<Category> findCategoryById(Integer id) {
        return categoryRepository.findById(id);
    }

    // (Admin) Tạo category mới
    @Transactional
    public Category createCategory(CategoryRequest dto) {
        Category category = new Category();
        category.setName(dto.getName());
        // (Xử lý parentId nếu entity Category của bạn có trường parentId)
        // if (dto.getParentId() != null) {
        //     category.setParentId(dto.getParentId()); 
        // }
        return categoryRepository.save(category);
    }

    // (Admin) Cập nhật category
    @Transactional
    public Category updateCategory(Integer id, CategoryRequest dto) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục với ID: " + id));
        
        existing.setName(dto.getName());
        // (Xử lý parentId nếu entity Category của bạn có trường parentId)
        // existing.setParentId(dto.getParentId()); 

        return categoryRepository.save(existing);
    }

    // (Admin) Xóa category
    @Transactional
    public void deleteCategory(Integer id) {
        // (Kiểm tra xem category có tồn tại không trước khi xóa)
        if (!categoryRepository.existsById(id)) {
             throw new IllegalArgumentException("Không tìm thấy danh mục với ID: " + id);
        }
        
        // (QUAN TRỌNG: Thêm kiểm tra nếu không muốn xóa danh mục đang có sản phẩm)
        // Ví dụ: Cần thêm hàm countByCategoryId trong ProductRepository
        // long productCount = productRepository.countByCategoryId(id); 
        // if (productCount > 0) {
        //     throw new IllegalStateException("Không thể xóa danh mục đang chứa " + productCount + " sản phẩm.");
        // }

        categoryRepository.deleteById(id);
    }
}
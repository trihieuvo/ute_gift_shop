package com.utegiftshop.repository;

import com.utegiftshop.entity.ShippingMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Kế thừa JpaRepository với Entity là ShippingMethod và kiểu dữ liệu của khóa chính là Integer
@Repository
public interface ShippingMethodRepository extends JpaRepository<ShippingMethod, Integer> {
    // Spring Data JPA sẽ tự động cung cấp các hàm CRUD cơ bản (findAll, save, deleteById, findById)
    // Bạn không cần viết thêm code nào ở đây.
}
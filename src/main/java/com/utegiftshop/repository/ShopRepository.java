package com.utegiftshop.repository;

import java.util.List;

import com.utegiftshop.entity.Shop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {
    /**
     * Tìm cửa hàng (Shop) dựa trên ID của người dùng (User)
     */
    Optional<Shop> findByUserId(Long userId);
    List<Shop> findByStatus(String status);
    // Thêm hàm đếm theo trạng thái (status)
    long countByStatus(String status);
}
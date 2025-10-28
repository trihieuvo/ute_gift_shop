package com.utegiftshop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.utegiftshop.entity.RoleApplication;

@Repository
public interface RoleApplicationRepository extends JpaRepository<RoleApplication, Long> {

    // Tìm các đơn đăng ký theo trạng thái
    List<RoleApplication> findByStatus(String status);

    // Kiểm tra xem user đã có đơn nào đang chờ duyệt chưa
    boolean existsByUserIdAndStatus(Long userId, String status);
    
    // Tìm đơn đang chờ duyệt của một user
    Optional<RoleApplication> findByUserIdAndStatus(Long userId, String status);
}
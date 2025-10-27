package com.utegiftshop.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.utegiftshop.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
    List<User> findByRoleName(String roleName);
    // THÊM: Đếm user mới được tạo trong khoảng thời gian
    long countByCreatedAtBetween(Timestamp createdAtStart, Timestamp createdAtEnd);
}

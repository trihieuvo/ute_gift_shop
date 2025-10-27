package com.utegiftshop.security.service;

import com.utegiftshop.entity.User;
import com.utegiftshop.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException; // Import thêm
import org.springframework.http.HttpStatus; // Import thêm

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // (Admin) Lấy tất cả user (có thể cần phân trang sau này)
    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        // Tạm thời lấy hết, bạn có thể thêm Pageable để phân trang
        return userRepository.findAll();
    }

    // (Admin) Lấy chi tiết 1 user (nếu cần)
    @Transactional(readOnly = true)
    public User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy user với ID: " + id));
    }

    // (Admin) Khóa hoặc Mở khóa tài khoản
    @Transactional
    public User toggleUserStatus(Long id) {
        User user = findUserById(id); // Gọi lại hàm trên để kiểm tra tồn tại

        // Đảo ngược trạng thái isActive
        user.setActive(!user.isActive());

        return userRepository.save(user);
    }

    // (Có thể bạn đã có các hàm khác như findByEmail, saveUser...)
}
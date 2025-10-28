package com.utegiftshop.security.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utegiftshop.entity.Role;
import com.utegiftshop.entity.Shop;
import com.utegiftshop.entity.User;
import com.utegiftshop.repository.RoleRepository;
import com.utegiftshop.repository.ShopRepository;
import com.utegiftshop.repository.UserRepository;

// (Import ShopRegistrationRequest nếu bạn có)

@Service
public class ShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Autowired
    public ShopService(ShopRepository shopRepository, 
                       UserRepository userRepository, 
                       RoleRepository roleRepository) {
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    // --- Chức năng cho ADMIN (UC-006) ---

    /**
     * Dùng cho ADMIN: Lấy danh sách các shop "Chờ phê duyệt".
     * Được gọi bởi AdminShopApiController.
     */
    @Transactional(readOnly = true)
    public List<Shop> findPendingShops() {
        return shopRepository.findByStatus("PENDING");
    }

    /**
     * Dùng cho ADMIN (UC-006): Phê duyệt một shop.
     * Được gọi bởi AdminShopApiController.
     */
    @Transactional // Đảm bảo tất cả các bước thành công
    public void approveShop(Long shopId) {
        // 1. Tìm Shop & User
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy shop với ID: " + shopId));
        
        User user = shop.getUser();
         if (user == null) {
             throw new RuntimeException("Cửa hàng không có chủ sở hữu.");
         }

        // 2. Tìm Role "Vendor" (Sửa lỗi: dùng "Vendor", không phải "ROLE_VENDOR")
        Role vendorRole = roleRepository.findByName("Vendor") 
                .orElseThrow(() -> new RuntimeException("Lỗi hệ thống: Không tìm thấy vai trò 'Vendor'. Hãy chắc chắn Role 'Vendor' tồn tại trong CSDL."));
        
        // 3. Nâng cấp vai trò User -> Vendor
        user.setRole(vendorRole);
        userRepository.save(user);

        // 4. Cập nhật trạng thái Shop -> ACTIVE và Gán Chiết khấu 0%
        shop.setStatus("ACTIVE");
        shop.setCommissionRate(BigDecimal.ZERO); // Đặt chiết khấu mặc định
        shopRepository.save(shop);
        
        // (Tùy chọn: Gửi email thông báo cho Vendor)
    }

    /**
     * Dùng cho ADMIN (UC-006): Từ chối một shop.
     * Được gọi bởi AdminShopApiController.
     */
    @Transactional
    public void rejectShop(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy shop với ID: " + shopId));

        shop.setStatus("REJECTED");
        shopRepository.save(shop);
        // (Tùy chọn: Gửi email thông báo cho User)
    }
    
    // --- Các chức năng khác (ví dụ: Customer đăng ký shop) ---
    // Giữ nguyên các hàm registerNewShop, findShopByUserId nếu bạn có
    // Ví dụ:
    
    /*
    @Transactional
    public Shop registerNewShop(ShopRegistrationRequest dto, Long userId) {
        // ... (Logic đăng ký shop của bạn)
    }
    
    @Transactional(readOnly = true)
    public Optional<Shop> findShopByUserId(Long userId) {
        return shopRepository.findByUserId(userId);
    }
    */
}
package com.utegiftshop.security.service;

import com.utegiftshop.dto.request.ShopRegistrationRequest; // (Bạn cần tạo DTO này)
import com.utegiftshop.entity.Role;
import com.utegiftshop.entity.Shop;
import com.utegiftshop.entity.User;
import com.utegiftshop.repository.RoleRepository;
import com.utegiftshop.repository.ShopRepository;
import com.utegiftshop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    // --- Chức năng cho CUSTOMER (UC-004) ---

    /**
     * Dùng cho UC-004: Customer gửi yêu cầu đăng ký shop.
     * Được gọi bởi một API (ví dụ: /api/shops/register)
     */
    @Transactional
    public Shop registerNewShop(ShopRegistrationRequest dto, Long userId) {
        // 1. Kiểm tra xem user này đã đăng ký shop chưa
        if (shopRepository.findByUserId(userId).isPresent()) {
            throw new IllegalStateException("Mỗi người dùng chỉ được đăng ký một cửa hàng.");
        }

        // 2. Lấy thông tin user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // 3. Tạo shop mới
        Shop shop = new Shop();
        shop.setUser(user);
        shop.setName(dto.getName());
        shop.setDescription(dto.getDescription());
        // (Bạn có thể thêm logoUrl, address... nếu DTO có)

        // 4. Set trạng thái PENDING
        shop.setStatus("PENDING"); 
        
        // (Giả sử Entity Shop của bạn có @CreationTimestamp nên không cần setCreatedAt)

        // 5. Lưu vào CSDL
        return shopRepository.save(shop);
    }

    /**
     * Dùng cho trang profile.html để kiểm tra trạng thái shop.
     * Được gọi bởi API (ví dụ: /api/shops/my)
     */
    @Transactional(readOnly = true)
    public Optional<Shop> findShopByUserId(Long userId) {
        return shopRepository.findByUserId(userId);
    }


    // --- Chức năng cho ADMIN (UC-006) ---

    /**
     * Dùng cho ADMIN (UC-006): Lấy danh sách các shop "Chờ phê duyệt".
     * Được gọi bởi AdminShopApiController.
     */
    @Transactional(readOnly = true)
    public List<Shop> findPendingShops() {
        // Sơ đồ tuần tự sd UC06
        return shopRepository.findByStatus("PENDING");
    }

    /**
     * Dùng cho ADMIN (UC-006): Phê duyệt một shop.
     * Được gọi bởi AdminShopApiController.
     */
    @Transactional
    public void approveShop(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy shop với ID: " + shopId));

        // 1. Cập nhật trạng thái Shop -> ACTIVE
        shop.setStatus("ACTIVE");
        shopRepository.save(shop);

        // 2. Nâng cấp vai trò User -> ROLE_VENDOR
        User user = shop.getUser();
        Role vendorRole = roleRepository.findByName("ROLE_VENDOR")
                .orElseThrow(() -> new RuntimeException("Lỗi hệ thống: Không tìm thấy ROLE_VENDOR"));
        
        user.setRole(vendorRole); // (Giả sử User có hàm setRole())
        userRepository.save(user);
    }

    /**
     * Dùng cho ADMIN (UC-006): Từ chối một shop.
     * Được gọi bởi AdminShopApiController.
     */
    @Transactional
    public void rejectShop(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy shop với ID: " + shopId));

        // Cập nhật trạng thái Shop -> REJECTED
        shop.setStatus("REJECTED");
        shopRepository.save(shop);
    }
}
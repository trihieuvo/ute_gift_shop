package com.utegiftshop.repository;

import com.utegiftshop.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional; // BỔ SUNG

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // === DÙNG CHO SHIPPER (ĐÃ CÓ) ===
    List<Order> findByShipperIdAndStatusIn(Long shipperId, List<String> statuses);
    long countByShipperIdAndStatus(Long shipperId, String status);
    long countByShipperIdAndStatusIn(Long shipperId, List<String> statuses);

    // === BỔ SUNG: DÙNG CHO CUSTOMER ===
    /**
     * Tìm đơn hàng theo ID của người dùng (Customer)
     */
    List<Order> findByUserId(Long userId);
    
    /**
     * Tìm đơn hàng theo ID đơn hàng VÀ ID người dùng (Để kiểm tra bảo mật)
     */
    Optional<Order> findByIdAndUserId(Long orderId, Long userId);
}
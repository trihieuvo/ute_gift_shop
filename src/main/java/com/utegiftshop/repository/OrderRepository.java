package com.utegiftshop.repository;

import com.utegiftshop.entity.Order;
import org.springframework.data.domain.Page; // BỔ SUNG
import org.springframework.data.domain.Pageable; // BỔ SUNG
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // BỔ SUNG
import org.springframework.data.repository.query.Param; // BỔ SUNG
import org.springframework.stereotype.Repository;

import java.math.BigDecimal; // BỔ SUNG
import java.sql.Timestamp; // BỔ SUNG
import java.util.List;
import java.util.Optional; 

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // === DÙNG CHO SHIPPER (ĐÃ CÓ) ===
    List<Order> findByShipperIdAndStatusIn(Long shipperId, List<String> statuses);
    long countByShipperIdAndStatus(Long shipperId, String status);
    long countByShipperIdAndStatusIn(Long shipperId, List<String> statuses);

    // === BỔ SUNG: DÙNG CHO PHÂN TRANG & LỌC LỊCH SỬ ĐƠN HÀNG ===
    Page<Order> findByShipperIdAndStatusIn(Long shipperId, List<String> statuses, Pageable pageable);
    
    Page<Order> findByShipperIdAndStatusInAndOrderDateBetween(
        Long shipperId, 
        List<String> statuses, 
        Timestamp orderDateStart, 
        Timestamp orderDateEnd, 
        Pageable pageable
    );
    // === KẾT THÚC BỔ SUNG PHÂN TRANG ===


    // === BỔ SUNG: TÍNH TỔNG TIỀN COD ĐANG GIỮ ===
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.shipper.id = :shipperId AND o.status = 'DELIVERED' AND o.paymentMethod = 'COD' AND o.isCodReconciled = false")
    BigDecimal sumTotalCodByShipperAndStatusDeliveredAndNotReconciled(@Param("shipperId") Long shipperId);
    // === KẾT THÚC BỔ SUNG COD ===


    // === DÙNG CHO CUSTOMER (GIỮ NGUYÊN) ===
    List<Order> findByUserId(Long userId);
    Optional<Order> findByIdAndUserId(Long orderId, Long userId);

    // Thêm hàm tính tổng tiền của các đơn hàng có trạng thái "DELIVERED"
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'DELIVERED'")
    BigDecimal sumTotalAmountIfStatusDelivered();
}
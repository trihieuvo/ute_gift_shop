package com.utegiftshop.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
// THÊM CÁC IMPORT NÀY
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
// KẾT THÚC THÊM IMPORT

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utegiftshop.dto.response.VendorDashboardStatsDto;
import com.utegiftshop.entity.Shop;
import com.utegiftshop.repository.ShopRepository;
import com.utegiftshop.security.service.UserDetailsImpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;


@RestController
@RequestMapping("/api/vendor/dashboard")
@PreAuthorize("hasAuthority('Vendor')")
public class VendorStatsController {

    private static final Logger logger = LoggerFactory.getLogger(VendorStatsController.class);

    @Autowired
    private ShopRepository shopRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private static final int LOW_STOCK_THRESHOLD = 5;

    // Hàm helper này không cần @Transactional
    private Shop getAuthenticatedShop(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) throw new RuntimeException("Xác thực lỗi.");
        Long userId = userDetails.getId();
        return shopRepository.findByUserId(userId)
                .orElseThrow(() -> {
                     logger.error("Shop Not Found user ID: {}", userId);
                     return new RuntimeException("Cửa hàng chưa được thiết lập.");
                 });
    }

    /**
     * API này (public) KHÔNG NÊN CÓ @Transactional.
     * Nó sẽ gọi hàm private bên dưới (có @Transactional) để thực hiện truy vấn.
     */
    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getDashboardStats(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        Shop shop;
        try {
            // 1. Kiểm tra Shop (và lỗi) BÊN NGOÀI transaction
            shop = getAuthenticatedShop(userDetails);
        } catch (RuntimeException shopNotFoundEx) {
             // 2. Nếu không tìm thấy shop, trả về lỗi 404 (NOT_FOUND) một cách an toàn
             logger.error("[getDashboardStats] Error: {}", shopNotFoundEx.getMessage());
              Map<String, String> errorBody = new HashMap<>();
              errorBody.put("message", shopNotFoundEx.getMessage());
             return ResponseEntity.status(HttpStatus.NOT_FOUND) // Sử dụng 404
                                  .contentType(MediaType.APPLICATION_JSON)
                                  .body(errorBody);
        }

        try {
            // 3. Shop hợp lệ, GỌI hàm private CÓ @Transactional
            VendorDashboardStatsDto statsDto = getTransactionalStats(shop.getId());
             return ResponseEntity.ok(statsDto);
             
        } catch (Exception e) {
            // 4. Bắt các lỗi khác (nếu có) từ hàm truy vấn
            logger.error("[getDashboardStats] Unexpected error while fetching stats:", e);
             Map<String, String> errorBody = new HashMap<>();
             errorBody.put("message", "Lỗi máy chủ khi tính thống kê: " + e.getMessage());
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                  .contentType(MediaType.APPLICATION_JSON)
                                  .body(errorBody);
        }
    }

    /**
     * Private transactional method to calculate dashboard stats
     * FIXED: Xử lý commissionRate NULL và Lọc doanh thu theo NĂM NAY
     */
    @Transactional(readOnly = true)
    public VendorDashboardStatsDto getTransactionalStats(Long shopId) { // Đổi tên hàm để tránh xung đột
        logger.info("[getTransactionalStats] Calculating stats for Shop ID: {}", shopId);
        Long newOrdersCount = 0L;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        Long lowStockCount = 0L;

        // 1. Count new orders (Không thay đổi)
        String newOrdersJpql = "SELECT COALESCE(COUNT(DISTINCT o.id), 0) FROM Order o " +
                               "JOIN o.orderDetails od " +
                               "JOIN od.product p " +
                               "WHERE p.shop.id = :shopId AND o.status = 'NEW'";
        try {
            newOrdersCount = entityManager.createQuery(newOrdersJpql, Long.class)
                                          .setParameter("shopId", shopId)
                                          .getSingleResult();
            logger.info("[getTransactionalStats] New Orders Count: {}", newOrdersCount);
        } catch (Exception e) {
            logger.error("[getTransactionalStats] Error counting new orders: {}", e.getMessage(), e);
            newOrdersCount = 0L;
        }

        // --- 2. TÍNH TỔNG DOANH THU RÒNG (TRONG NĂM NAY) ---
        
        // **THÊM MỚI**: Xác định khoảng thời gian của năm nay
        LocalDateTime now = LocalDateTime.now();
        Timestamp startOfYear = Timestamp.valueOf(now.with(TemporalAdjusters.firstDayOfYear()).toLocalDate().atStartOfDay());
        Timestamp endOfYear = Timestamp.valueOf(now.with(TemporalAdjusters.lastDayOfYear()).toLocalDate().atTime(LocalTime.MAX));
        
        // **CẬP NHẬT**: Thêm bộ lọc ngày (AND o.orderDate BETWEEN :startDate AND :endDate)
        String totalRevenueJpql = "SELECT COALESCE(SUM( " +
                                  "(od.price * od.quantity) * (1.0 - (COALESCE(od.commissionRate, 0.0) / 100.0)) " +
                                  "), 0.0) FROM OrderDetail od " +
                                  "JOIN od.order o " +
                                  "JOIN od.product p " +
                                  "WHERE p.shop.id = :shopId " +
                                  "AND o.status = 'DELIVERED' " +
                                  "AND o.orderDate BETWEEN :startDate AND :endDate"; // <-- Thêm dòng này

        try {
            // **CẬP NHẬT**: Truy vấn về Double và thêm tham số ngày
            Double result = entityManager.createQuery(totalRevenueJpql, Double.class)
                                        .setParameter("shopId", shopId)
                                        .setParameter("startDate", startOfYear) // <-- Thêm tham số
                                        .setParameter("endDate", endOfYear)     // <-- Thêm tham số
                                        .getSingleResult();
            
            totalRevenue = BigDecimal.valueOf(result != null ? result : 0.0);
            
            logger.info("[getTransactionalStats] Total NET Revenue (This Year) Calculated: {}", totalRevenue);
        } catch (NoResultException e) {
            logger.warn("[getTransactionalStats] No delivered orders found (This Year) for Shop ID: {}", shopId);
            totalRevenue = BigDecimal.ZERO;
        } catch (Exception e) {
            logger.error("[getTransactionalStats] Error calculating revenue (This Year): {}", e.getMessage(), e);
            totalRevenue = BigDecimal.ZERO;
        }
        // --- KẾT THÚC TÍNH DOANH THU RÒNG ---

        // 3. Count low stock products (Không thay đổi)
        String lowStockJpql = "SELECT COALESCE(COUNT(p.id), 0) FROM Product p " +
                              "WHERE p.shop.id = :shopId AND p.stockQuantity <= :threshold";
        try {
            lowStockCount = entityManager.createQuery(lowStockJpql, Long.class)
                                         .setParameter("shopId", shopId)
                                         .setParameter("threshold", LOW_STOCK_THRESHOLD)
                                         .getSingleResult();
            logger.info("[getTransactionalStats] Low Stock Count (<= {}): {}", LOW_STOCK_THRESHOLD, lowStockCount);
        } catch (Exception e) {
            logger.error("[getTransactionalStats] Error counting low stock: {}", e.getMessage(), e);
            lowStockCount = 0L;
        }

        // 4. Create and return DTO
        return new VendorDashboardStatsDto(
            newOrdersCount,
            totalRevenue, // Đây là doanh thu RÒNG (trong năm nay)
            lowStockCount
        );
    }
}
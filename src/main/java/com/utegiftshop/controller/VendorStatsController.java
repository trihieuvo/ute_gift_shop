package com.utegiftshop.controller;

import com.utegiftshop.dto.response.VendorDashboardStatsDto;
import com.utegiftshop.entity.Shop;
import com.utegiftshop.repository.ShopRepository;
import com.utegiftshop.security.service.UserDetailsImpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

    private Shop getAuthenticatedShop(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) throw new RuntimeException("Xác thực lỗi.");
        Long userId = userDetails.getId();
        return shopRepository.findByUserId(userId)
                .orElseThrow(() -> {
                     logger.error("Shop Not Found user ID: {}", userId);
                     return new RuntimeException("Cửa hàng chưa được thiết lập.");
                 });
    }

    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<?> getDashboardStats(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Shop shop = getAuthenticatedShop(userDetails);
            Long shopId = shop.getId();
            logger.info("[getDashboardStats] Calculating stats for Shop ID: {}", shopId);
            
            // 1. Đếm đơn hàng mới (phần này sẽ hoạt động)
            String newOrdersJpql = "SELECT COALESCE(COUNT(DISTINCT o.id), 0) FROM Orders o " +
                                   "JOIN o.orderDetails od " +
                                   "JOIN od.product p " +
                                   "WHERE p.shop.id = :shopId AND o.status = 'NEW'";
            Long newOrdersCount = entityManager.createQuery(newOrdersJpql, Long.class)
                                              .setParameter("shopId", shopId)
                                              .getSingleResult();
            logger.info("[getDashboardStats] New Orders Count: {}", newOrdersCount);

            // --- VÔ HIỆU HÓA TẠM THỜI PHẦN TÍNH DOANH THU ---
            /* 
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
            
            String todayRevenueJpql = "SELECT COALESCE(SUM(od.price * od.quantity), 0.0) FROM OrderDetail od " +
                                      "JOIN od.orders o " +
                                      "JOIN od.product p " +
                                      "WHERE p.shop.id = :shopId " +
                                      "AND o.status = 'DELIVERED' " +
                                      "AND o.orderDate BETWEEN :startOfDay AND :endOfDay";
            Double revenueResult = entityManager.createQuery(todayRevenueJpql, Double.class)
                                                .setParameter("shopId", shopId)
                                                .setParameter("startOfDay", startOfDay)
                                                .setParameter("endOfDay", endOfDay)
                                                .getSingleResult();
            BigDecimal todayRevenue = BigDecimal.valueOf(revenueResult);
            */
            // Thay thế bằng giá trị mặc định là 0
            BigDecimal todayRevenue = BigDecimal.ZERO; 
            logger.info("[getDashboardStats] Today Revenue (temporarily disabled): {}", todayRevenue);
            // --- KẾT THÚC PHẦN VÔ HIỆU HÓA ---

            // 3. Đếm sản phẩm sắp hết (phần này sẽ hoạt động)
            String lowStockJpql = "SELECT COALESCE(COUNT(p.id), 0) FROM Product p " +
                                  "WHERE p.shop.id = :shopId AND p.stockQuantity <= :threshold";
            Long lowStockCount = entityManager.createQuery(lowStockJpql, Long.class)
                                             .setParameter("shopId", shopId)
                                             .setParameter("threshold", LOW_STOCK_THRESHOLD)
                                             .getSingleResult();
            logger.info("[getDashboardStats] Low Stock Count (<= {}): {}", LOW_STOCK_THRESHOLD, lowStockCount);

            // 4. Create DTO and return
            VendorDashboardStatsDto statsDto = new VendorDashboardStatsDto(
                newOrdersCount,
                todayRevenue,
                lowStockCount
            );
            return ResponseEntity.ok(statsDto);

        } catch (Exception e) {
            logger.error("[getDashboardStats] Unexpected error while fetching stats:", e); 
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body("{\"message\": \"Lỗi máy chủ khi tính thống kê: " + e.getMessage() + "\"}");
        }
    }
}
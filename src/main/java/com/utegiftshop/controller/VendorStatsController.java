package com.utegiftshop.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

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

    private Shop getAuthenticatedShop(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        // ... (Giữ nguyên phần này)
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
        // Đổi tên biến cho rõ ràng hơn (tùy chọn)
        BigDecimal totalRevenue = BigDecimal.ZERO;
        Long newOrdersCount = 0L;
        Long lowStockCount = 0L;

        try {
            Shop shop = getAuthenticatedShop(userDetails);
            Long shopId = shop.getId();
            logger.info("[getDashboardStats] Calculating stats for Shop ID: {}", shopId);

            // 1. Đếm đơn hàng mới (Giữ nguyên)
            String newOrdersJpql = "SELECT COALESCE(COUNT(DISTINCT o.id), 0) FROM Order o " +
                                   "JOIN o.orderDetails od " +
                                   "JOIN od.product p " +
                                   "WHERE p.shop.id = :shopId AND o.status = 'NEW'";
            newOrdersCount = entityManager.createQuery(newOrdersJpql, Long.class)
                                          .setParameter("shopId", shopId)
                                          .getSingleResult();
            logger.info("[getDashboardStats] New Orders Count: {}", newOrdersCount);

            // --- TÍNH TỔNG DOANH THU (ALL TIME) ---
            // Chỉ cần lọc theo shopId và status 'DELIVERED', bỏ điều kiện ngày
            String totalRevenueJpql = "SELECT COALESCE(SUM(od.price * od.quantity), 0.0) FROM OrderDetail od " +
                                      "JOIN od.order o " +
                                      "JOIN od.product p " +
                                      "WHERE p.shop.id = :shopId " +
                                      "AND o.status = 'DELIVERED'"; // Bỏ phần AND o.orderDate BETWEEN ...

            try {
                // Vẫn mong đợi kết quả là BigDecimal
                totalRevenue = entityManager.createQuery(totalRevenueJpql, BigDecimal.class)
                                            .setParameter("shopId", shopId)
                                            // Không cần setParameter cho ngày nữa
                                            .getSingleResult();
                if (totalRevenue == null) {
                    totalRevenue = BigDecimal.ZERO;
                }
            } catch (NoResultException e) {
                logger.warn("[getDashboardStats] No delivered orders found at all for Shop ID: {}", shopId);
                totalRevenue = BigDecimal.ZERO;
            } catch (Exception e) {
                 logger.error("[getDashboardStats] Error calculating total revenue for Shop ID {}: {}", shopId, e.getMessage());
                 totalRevenue = BigDecimal.ZERO;
            }

            logger.info("[getDashboardStats] Total Revenue Calculated: {}", totalRevenue);
            // --- KẾT THÚC TÍNH TỔNG DOANH THU ---

            // 3. Đếm sản phẩm sắp hết (Giữ nguyên)
            String lowStockJpql = "SELECT COALESCE(COUNT(p.id), 0) FROM Product p " +
                                  "WHERE p.shop.id = :shopId AND p.stockQuantity <= :threshold";
            lowStockCount = entityManager.createQuery(lowStockJpql, Long.class)
                                         .setParameter("shopId", shopId)
                                         .setParameter("threshold", LOW_STOCK_THRESHOLD)
                                         .getSingleResult();
            logger.info("[getDashboardStats] Low Stock Count (<= {}): {}", LOW_STOCK_THRESHOLD, lowStockCount);

            // 4. Create DTO and return
            // Đảm bảo tên biến trong DTO khớp (todayRevenue -> totalRevenue nếu bạn đổi tên DTO)
            // Nếu DTO vẫn là VendorDashboardStatsDto(long newOrdersCount, BigDecimal todayRevenue, long lowStockCount)
            // thì bạn truyền totalRevenue vào vị trí thứ 2
            VendorDashboardStatsDto statsDto = new VendorDashboardStatsDto(
                newOrdersCount,
                totalRevenue, // Truyền tổng doanh thu vào đây
                lowStockCount
            );
            return ResponseEntity.ok(statsDto);

        } catch (RuntimeException shopNotFoundEx) {
             // ... (Giữ nguyên phần xử lý lỗi này)
             logger.error("[getDashboardStats] Error: {}", shopNotFoundEx.getMessage());
              Map<String, String> errorBody = new HashMap<>();
              errorBody.put("message", shopNotFoundEx.getMessage());
             return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                  .contentType(MediaType.APPLICATION_JSON)
                                  .body(errorBody);
        } catch (Exception e) {
            // ... (Giữ nguyên phần xử lý lỗi này)
            logger.error("[getDashboardStats] Unexpected error while fetching stats:", e);
             Map<String, String> errorBody = new HashMap<>();
             errorBody.put("message", "Lỗi máy chủ khi tính thống kê: " + e.getMessage());
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                  .contentType(MediaType.APPLICATION_JSON)
                                  .body(errorBody);
        }
    }
}
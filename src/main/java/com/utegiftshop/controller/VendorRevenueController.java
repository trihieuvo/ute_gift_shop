package com.utegiftshop.controller;

import com.utegiftshop.entity.Shop;
import com.utegiftshop.repository.ShopRepository;
import com.utegiftshop.security.service.UserDetailsImpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vendor/stats")
@PreAuthorize("hasAuthority('Vendor')")
public class VendorRevenueController {

    private static final Logger logger = LoggerFactory.getLogger(VendorRevenueController.class);

    @Autowired
    private ShopRepository shopRepository;

    @PersistenceContext
    private EntityManager entityManager;

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
     * Endpoint specifically for Revenue Statistics (Daily, Monthly, Yearly)
     */
    @GetMapping(value = "/revenue", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<?> getRevenueStats(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Shop shop = getAuthenticatedShop(userDetails);
            Long shopId = shop.getId();
            logger.info("[getRevenueStats] Calculating revenue for Shop ID: {}", shopId);

            // --- Calculate Date Ranges ---
            LocalDateTime now = LocalDateTime.now();
            // Daily
            Timestamp startOfDay = Timestamp.valueOf(now.toLocalDate().atStartOfDay());
            Timestamp endOfDay = Timestamp.valueOf(now.toLocalDate().atTime(LocalTime.MAX));
            // Monthly
            Timestamp startOfMonth = Timestamp.valueOf(now.with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atStartOfDay());
            Timestamp endOfMonth = Timestamp.valueOf(now.with(TemporalAdjusters.lastDayOfMonth()).toLocalDate().atTime(LocalTime.MAX));
            // Yearly
            Timestamp startOfYear = Timestamp.valueOf(now.with(TemporalAdjusters.firstDayOfYear()).toLocalDate().atStartOfDay());
            Timestamp endOfYear = Timestamp.valueOf(now.with(TemporalAdjusters.lastDayOfYear()).toLocalDate().atTime(LocalTime.MAX));

            // --- JPQL Query ---
            String revenueJpql = "SELECT COALESCE(SUM(od.price * od.quantity), 0.0) FROM OrderDetail od " +
                                 "JOIN od.order o " +
                                 "JOIN od.product p " +
                                 "WHERE p.shop.id = :shopId " +
                                 "AND o.status = 'DELIVERED' " +
                                 "AND o.orderDate BETWEEN :startDate AND :endDate";

            // --- Execute Queries ---
            BigDecimal dailyRevenue = executeRevenueQuery(revenueJpql, shopId, startOfDay, endOfDay);
            BigDecimal monthlyRevenue = executeRevenueQuery(revenueJpql, shopId, startOfMonth, endOfMonth);
            BigDecimal yearlyRevenue = executeRevenueQuery(revenueJpql, shopId, startOfYear, endOfYear);

            logger.info("[getRevenueStats] Daily: {}, Monthly: {}, Yearly: {} for Shop ID: {}",
                        dailyRevenue, monthlyRevenue, yearlyRevenue, shopId);

            // --- Prepare Response ---
            Map<String, BigDecimal> revenueData = new HashMap<>();
            revenueData.put("daily", dailyRevenue);
            revenueData.put("monthly", monthlyRevenue);
            revenueData.put("yearly", yearlyRevenue);

            return ResponseEntity.ok(revenueData);

        } catch (RuntimeException e) {
            logger.error("[getRevenueStats] Error fetching revenue stats: {}", e.getMessage());
             Map<String, String> errorBody = new HashMap<>();
             errorBody.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(errorBody);
        } catch (Exception e) {
            logger.error("[getRevenueStats] Unexpected error while fetching revenue stats:", e);
             Map<String, String> errorBody = new HashMap<>();
             errorBody.put("message", "Lỗi máy chủ khi tính doanh thu: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(errorBody);
        }
    }

    /**
     * ========== API MỚI 1: DOANH THU THEO NGÀY ==========
     * GET /api/vendor/stats/revenue/daily?days=7
     * Trả về doanh thu từng ngày trong X ngày gần nhất
     */
    @GetMapping(value = "/revenue/daily", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<?> getDailyRevenueChart(
            @RequestParam(defaultValue = "7") int days,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        try {
            Shop shop = getAuthenticatedShop(userDetails);
            Long shopId = shop.getId();
            logger.info("[getDailyRevenueChart] Calculating daily revenue for {} days, Shop ID: {}", days, shopId);

            // Validate input
            if (days < 1 || days > 90) {
                days = 7; // Default to 7 days if invalid
            }

            List<Map<String, Object>> dailyRevenueList = new ArrayList<>();
            LocalDate today = LocalDate.now();

            String revenueJpql = "SELECT COALESCE(SUM(od.price * od.quantity), 0.0) FROM OrderDetail od " +
                                 "JOIN od.order o " +
                                 "JOIN od.product p " +
                                 "WHERE p.shop.id = :shopId " +
                                 "AND o.status = 'DELIVERED' " +
                                 "AND o.orderDate BETWEEN :startDate AND :endDate";

            // Loop through each day
            for (int i = days - 1; i >= 0; i--) {
                LocalDate targetDate = today.minusDays(i);
                Timestamp startOfDay = Timestamp.valueOf(targetDate.atStartOfDay());
                Timestamp endOfDay = Timestamp.valueOf(targetDate.atTime(LocalTime.MAX));

                BigDecimal revenue = executeRevenueQuery(revenueJpql, shopId, startOfDay, endOfDay);

                Map<String, Object> dailyData = new HashMap<>();
                dailyData.put("date", targetDate.toString()); // Format: "2025-10-22"
                dailyData.put("formattedDate", String.format("%02d/%02d", 
                    targetDate.getDayOfMonth(), targetDate.getMonthValue())); // Format: "22/10"
                dailyData.put("revenue", revenue);

                dailyRevenueList.add(dailyData);
            }

            logger.info("[getDailyRevenueChart] Successfully calculated {} daily revenue records for Shop ID: {}", 
                dailyRevenueList.size(), shopId);

            return ResponseEntity.ok(dailyRevenueList);

        } catch (RuntimeException e) {
            logger.error("[getDailyRevenueChart] Error: {}", e.getMessage());
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(errorBody);
        } catch (Exception e) {
            logger.error("[getDailyRevenueChart] Unexpected error:", e);
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("message", "Lỗi máy chủ: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(errorBody);
        }
    }

    /**
     * ========== API MỚI 2: DOANH THU THEO THÁNG ==========
     * GET /api/vendor/stats/revenue/monthly?months=12
     * Trả về doanh thu từng tháng trong X tháng gần nhất
     */
    @GetMapping(value = "/revenue/monthly", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMonthlyRevenueChart(
            @RequestParam(defaultValue = "12") int months,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        try {
            Shop shop = getAuthenticatedShop(userDetails);
            Long shopId = shop.getId();
            logger.info("[getMonthlyRevenueChart] Calculating monthly revenue for {} months, Shop ID: {}", months, shopId);

            // Validate input
            if (months < 1 || months > 24) {
                months = 12; // Default to 12 months if invalid
            }

            List<Map<String, Object>> monthlyRevenueList = new ArrayList<>();
            LocalDate today = LocalDate.now();

            String revenueJpql = "SELECT COALESCE(SUM(od.price * od.quantity), 0.0) FROM OrderDetail od " +
                                 "JOIN od.order o " +
                                 "JOIN od.product p " +
                                 "WHERE p.shop.id = :shopId " +
                                 "AND o.status = 'DELIVERED' " +
                                 "AND o.orderDate BETWEEN :startDate AND :endDate";

            // Loop through each month
            for (int i = months - 1; i >= 0; i--) {
                LocalDate targetMonth = today.minusMonths(i);
                LocalDate firstDayOfMonth = targetMonth.with(TemporalAdjusters.firstDayOfMonth());
                LocalDate lastDayOfMonth = targetMonth.with(TemporalAdjusters.lastDayOfMonth());

                Timestamp startOfMonth = Timestamp.valueOf(firstDayOfMonth.atStartOfDay());
                Timestamp endOfMonth = Timestamp.valueOf(lastDayOfMonth.atTime(LocalTime.MAX));

                BigDecimal revenue = executeRevenueQuery(revenueJpql, shopId, startOfMonth, endOfMonth);

                Map<String, Object> monthlyData = new HashMap<>();
                monthlyData.put("year", targetMonth.getYear());
                monthlyData.put("month", targetMonth.getMonthValue());
                monthlyData.put("formattedMonth", String.format("%02d/%d", 
                    targetMonth.getMonthValue(), targetMonth.getYear())); // Format: "10/2025"
                monthlyData.put("revenue", revenue);

                monthlyRevenueList.add(monthlyData);
            }

            logger.info("[getMonthlyRevenueChart] Successfully calculated {} monthly revenue records for Shop ID: {}", 
                monthlyRevenueList.size(), shopId);

            return ResponseEntity.ok(monthlyRevenueList);

        } catch (RuntimeException e) {
            logger.error("[getMonthlyRevenueChart] Error: {}", e.getMessage());
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(errorBody);
        } catch (Exception e) {
            logger.error("[getMonthlyRevenueChart] Unexpected error:", e);
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("message", "Lỗi máy chủ: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(errorBody);
        }
    }

    /**
     * Helper method to execute the revenue query.
     */
    private BigDecimal executeRevenueQuery(String jpql, Long shopId, Timestamp startDate, Timestamp endDate) {
        try {
            BigDecimal result = entityManager.createQuery(jpql, BigDecimal.class)
                                         .setParameter("shopId", shopId)
                                         .setParameter("startDate", startDate)
                                         .setParameter("endDate", endDate)
                                         .getSingleResult();
            return result != null ? result : BigDecimal.ZERO;
        } catch (NoResultException nre) {
             logger.warn("No revenue results for shopId={}, startDate={}, endDate={}", shopId, startDate, endDate);
            return BigDecimal.ZERO;
        } catch (Exception e) {
            logger.error("Error executing revenue query for shopId={}, startDate={}, endDate={}: {}",
                         shopId, startDate, endDate, e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}
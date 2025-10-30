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

    @GetMapping(value = "/revenue", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getRevenueStats(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        Shop shop;
        try {
            shop = getAuthenticatedShop(userDetails);
        } catch (RuntimeException e) {
            logger.error("[getRevenueStats] Error: {}", e.getMessage());
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(errorBody);
        }
        
        try {
            Map<String, BigDecimal> revenueData = getTransactionalRevenueStats(shop.getId());
            return ResponseEntity.ok(revenueData);

        } catch (Exception e) {
            logger.error("[getRevenueStats] Unexpected error:", e);
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("message", "Lỗi máy chủ: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(errorBody);
        }
    }

    @Transactional(readOnly = true)
    private Map<String, BigDecimal> getTransactionalRevenueStats(Long shopId) {
        logger.info("[getTransactionalRevenueStats] Calculating revenue for Shop ID: {}", shopId);

        LocalDateTime now = LocalDateTime.now();
        Timestamp startOfDay = Timestamp.valueOf(now.toLocalDate().atStartOfDay());
        Timestamp endOfDay = Timestamp.valueOf(now.toLocalDate().atTime(LocalTime.MAX));
        Timestamp startOfMonth = Timestamp.valueOf(now.with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atStartOfDay());
        Timestamp endOfMonth = Timestamp.valueOf(now.with(TemporalAdjusters.lastDayOfMonth()).toLocalDate().atTime(LocalTime.MAX));
        Timestamp startOfYear = Timestamp.valueOf(now.with(TemporalAdjusters.firstDayOfYear()).toLocalDate().atStartOfDay());
        Timestamp endOfYear = Timestamp.valueOf(now.with(TemporalAdjusters.lastDayOfYear()).toLocalDate().atTime(LocalTime.MAX));

        // FIX: Nhận về Double từ query, sau đó convert sang BigDecimal
        String revenueJpql = "SELECT COALESCE(SUM( " +
                             "(od.price * od.quantity) * (1.0 - (COALESCE(od.commissionRate, 0.0) / 100.0)) " +
                             "), 0.0) FROM OrderDetail od " +
                             "JOIN od.order o " +
                             "JOIN od.product p " +
                             "WHERE p.shop.id = :shopId " +
                             "AND o.status = 'DELIVERED' " +
                             "AND o.orderDate BETWEEN :startDate AND :endDate";

        BigDecimal dailyRevenue = executeRevenueQuery(revenueJpql, shopId, startOfDay, endOfDay);
        BigDecimal monthlyRevenue = executeRevenueQuery(revenueJpql, shopId, startOfMonth, endOfMonth);
        BigDecimal yearlyRevenue = executeRevenueQuery(revenueJpql, shopId, startOfYear, endOfYear);

        logger.info("[getTransactionalRevenueStats] Daily: {}, Monthly: {}, Yearly: {}", 
                    dailyRevenue, monthlyRevenue, yearlyRevenue);

        Map<String, BigDecimal> revenueData = new HashMap<>();
        revenueData.put("daily", dailyRevenue);
        revenueData.put("monthly", monthlyRevenue);
        revenueData.put("yearly", yearlyRevenue);

        return revenueData;
    }


    @GetMapping(value = "/revenue/daily", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getDailyRevenueChart(
            @RequestParam(defaultValue = "7") int days,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        Shop shop;
        try {
            shop = getAuthenticatedShop(userDetails);
        } catch (RuntimeException e) {
            logger.error("[getDailyRevenueChart] Error: {}", e.getMessage());
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(errorBody);
        }
        
        try {
            List<Map<String, Object>> dailyRevenueList = getTransactionalDailyRevenueChart(shop.getId(), days);
            return ResponseEntity.ok(dailyRevenueList);
        } catch (Exception e) {
            logger.error("[getDailyRevenueChart] Unexpected error:", e);
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("message", "Lỗi máy chủ: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(errorBody);
        }
    }

    @Transactional(readOnly = true)
    private List<Map<String, Object>> getTransactionalDailyRevenueChart(Long shopId, int days) {
        logger.info("[getTransactionalDailyRevenueChart] Calculating daily revenue for {} days, Shop ID: {}", days, shopId);

        if (days < 1 || days > 90) {
            days = 7;
        }

        List<Map<String, Object>> dailyRevenueList = new ArrayList<>();
        LocalDate today = LocalDate.now();

        String revenueJpql = "SELECT COALESCE(SUM( " +
                             "(od.price * od.quantity) * (1.0 - (COALESCE(od.commissionRate, 0.0) / 100.0)) " +
                             "), 0.0) FROM OrderDetail od " +
                             "JOIN od.order o " +
                             "JOIN od.product p " +
                             "WHERE p.shop.id = :shopId " +
                             "AND o.status = 'DELIVERED' " +
                             "AND o.orderDate BETWEEN :startDate AND :endDate";

        for (int i = days - 1; i >= 0; i--) {
            LocalDate targetDate = today.minusDays(i);
            Timestamp startOfDay = Timestamp.valueOf(targetDate.atStartOfDay());
            Timestamp endOfDay = Timestamp.valueOf(targetDate.atTime(LocalTime.MAX));

            BigDecimal revenue = executeRevenueQuery(revenueJpql, shopId, startOfDay, endOfDay);

            Map<String, Object> dailyData = new HashMap<>();
            dailyData.put("date", targetDate.toString());
            dailyData.put("formattedDate", String.format("%02d/%02d", 
                targetDate.getDayOfMonth(), targetDate.getMonthValue()));
            dailyData.put("revenue", revenue);

            dailyRevenueList.add(dailyData);
        }

        return dailyRevenueList;
    }


    @GetMapping(value = "/revenue/monthly", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMonthlyRevenueChart(
            @RequestParam(defaultValue = "12") int months,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        Shop shop;
        try {
            shop = getAuthenticatedShop(userDetails);
        } catch (RuntimeException e) {
            logger.error("[getMonthlyRevenueChart] Error: {}", e.getMessage());
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(errorBody);
        }
        
        try {
            List<Map<String, Object>> monthlyRevenueList = getTransactionalMonthlyRevenueChart(shop.getId(), months);
            return ResponseEntity.ok(monthlyRevenueList);

        } catch (Exception e) {
            logger.error("[getMonthlyRevenueChart] Unexpected error:", e);
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("message", "Lỗi máy chủ: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(errorBody);
        }
    }

    @Transactional(readOnly = true)
    private List<Map<String, Object>> getTransactionalMonthlyRevenueChart(Long shopId, int months) {
        logger.info("[getTransactionalMonthlyRevenueChart] Calculating monthly revenue for {} months, Shop ID: {}", months, shopId);

        if (months < 1 || months > 24) {
            months = 12;
        }

        List<Map<String, Object>> monthlyRevenueList = new ArrayList<>();
        LocalDate today = LocalDate.now();

        String revenueJpql = "SELECT COALESCE(SUM( " +
                             "(od.price * od.quantity) * (1.0 - (COALESCE(od.commissionRate, 0.0) / 100.0)) " +
                             "), 0.0) FROM OrderDetail od " +
                             "JOIN od.order o " +
                             "JOIN od.product p " +
                             "WHERE p.shop.id = :shopId " +
                             "AND o.status = 'DELIVERED' " +
                             "AND o.orderDate BETWEEN :startDate AND :endDate";

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
                targetMonth.getMonthValue(), targetMonth.getYear()));
            monthlyData.put("revenue", revenue);

            monthlyRevenueList.add(monthlyData);
        }

        return monthlyRevenueList;
    }


    /**
     * FIX: Query trả về Double, convert sang BigDecimal
     */
    private BigDecimal executeRevenueQuery(String jpql, Long shopId, Timestamp startDate, Timestamp endDate) {
        try {
            // Nhận về Double từ query
            Double result = entityManager.createQuery(jpql, Double.class)
                                         .setParameter("shopId", shopId)
                                         .setParameter("startDate", startDate)
                                         .setParameter("endDate", endDate)
                                         .getSingleResult();
            
            // Convert sang BigDecimal
            return result != null ? BigDecimal.valueOf(result) : BigDecimal.ZERO;
            
        } catch (NoResultException nre) {
            logger.warn("No revenue results for shopId={}, startDate={}, endDate={}", shopId, startDate, endDate);
            return BigDecimal.ZERO;
        } catch (Exception e) {
            logger.error("Error executing revenue query for shopId={}: {}", shopId, e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }
}
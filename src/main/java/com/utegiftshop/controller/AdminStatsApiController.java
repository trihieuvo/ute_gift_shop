package com.utegiftshop.controller;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.utegiftshop.dto.response.AdminDashboardStats;
import com.utegiftshop.entity.Order;
import com.utegiftshop.entity.OrderDetail;
import com.utegiftshop.repository.OrderRepository;
import com.utegiftshop.repository.ShopRepository;
import com.utegiftshop.repository.UserRepository;

@RestController
@RequestMapping("/api/v1/admin/stats")
public class AdminStatsApiController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;

    public AdminStatsApiController(OrderRepository orderRepository, UserRepository userRepository, ShopRepository shopRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardStats> getDashboardStats(
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate
    ) {
        Timestamp start = null;
        Timestamp end = null;
        
        try {
            if (startDate != null && !startDate.isEmpty()) start = Timestamp.valueOf(startDate + " 00:00:00");
            if (endDate != null && !endDate.isEmpty()) end = Timestamp.valueOf(endDate + " 23:59:59");
        } catch (IllegalArgumentException ignored) {}

        // Logic đếm người dùng mới và shop hoạt động
        long totalNewUsers = userRepository.countByCreatedAtBetween(start, end);
        long totalActiveShops = shopRepository.countByStatus("ACTIVE"); 
        
        // Lấy danh sách đơn hàng đã giao để tính toán doanh thu và hoa hồng
        List<Order> deliveredOrders;
        if (start != null && end != null) {
            deliveredOrders = orderRepository.findDeliveredOrdersBetween(start, end);
        } else if (start != null) {
            deliveredOrders = orderRepository.findDeliveredOrdersAfter(start);
        } else if (end != null) {
            deliveredOrders = orderRepository.findDeliveredOrdersBefore(end);
        } else {
            deliveredOrders = orderRepository.findAllDelivered();
        }
        
        long totalDeliveredOrders = deliveredOrders.size();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCommission = BigDecimal.ZERO;

        for (Order order : deliveredOrders) {
            if (order.getTotalAmount() != null) {
                totalRevenue = totalRevenue.add(order.getTotalAmount());
            }
            if (order.getOrderDetails() != null) {
                for (OrderDetail detail : order.getOrderDetails()) {
                    if (detail.getPrice() != null && detail.getQuantity() != null && detail.getCommissionRate() != null) {
                        BigDecimal itemTotal = detail.getPrice().multiply(new BigDecimal(detail.getQuantity()));
                        BigDecimal commissionRate = detail.getCommissionRate();
                        BigDecimal commissionForItem = itemTotal.multiply(commissionRate.divide(new BigDecimal("100")));
                        totalCommission = totalCommission.add(commissionForItem);
                    }
                }
            }
        }
        
        AdminDashboardStats stats = new AdminDashboardStats(
            totalNewUsers, 
            totalDeliveredOrders, 
            totalActiveShops, 
            totalRevenue,
            totalCommission
        );

        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/revenue-chart")
    public ResponseEntity<Map<String, BigDecimal>> getMonthlyRevenue(
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate
    ) {
        Timestamp start = null;
        Timestamp end = null;
        
        try {
            if (startDate != null && !startDate.isEmpty()) start = Timestamp.valueOf(startDate + " 00:00:00");
            if (endDate != null && !endDate.isEmpty()) end = Timestamp.valueOf(endDate + " 23:59:59");
        } catch (IllegalArgumentException ignored) {}

        List<Order> deliveredOrders;
        if (start != null && end != null) {
            deliveredOrders = orderRepository.findDeliveredOrdersBetween(start, end);
        } else if (start != null) {
            deliveredOrders = orderRepository.findDeliveredOrdersAfter(start);
        } else if (end != null) {
            deliveredOrders = orderRepository.findDeliveredOrdersBefore(end);
        } else {
            deliveredOrders = orderRepository.findAllDelivered();
        }

        // Nhóm doanh thu hoa hồng theo tháng và năm
        Map<String, BigDecimal> monthlyCommission = deliveredOrders.stream()
            .flatMap(order -> order.getOrderDetails().stream())
            .filter(detail -> detail.getPrice() != null && detail.getQuantity() != null && detail.getCommissionRate() != null)
            .collect(Collectors.groupingBy(
                detail -> "Tháng " + detail.getOrder().getOrderDate().toLocalDateTime().format(DateTimeFormatter.ofPattern("MM/yyyy")),
                LinkedHashMap::new, // Giữ thứ tự
                Collectors.mapping(
                    detail -> {
                        BigDecimal itemTotal = detail.getPrice().multiply(new BigDecimal(detail.getQuantity()));
                        BigDecimal commissionRate = detail.getCommissionRate();
                        return itemTotal.multiply(commissionRate.divide(new BigDecimal("100")));
                    },
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                )
            ));
        
        return ResponseEntity.ok(monthlyCommission);
    }
}
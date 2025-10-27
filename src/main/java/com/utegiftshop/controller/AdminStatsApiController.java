package com.utegiftshop.controller;

import com.utegiftshop.dto.response.AdminDashboardStats; // Sẽ tạo DTO này
import com.utegiftshop.repository.OrderRepository;
import com.utegiftshop.repository.ShopRepository;
import com.utegiftshop.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

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

    // API 1: Lấy các số liệu thống kê tổng quan
    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardStats> getDashboardStats() {
        // 1. Tổng số User (Giả định bạn có hàm count trong Repository)
        long totalUsers = userRepository.count();

        // 2. Tổng số Đơn hàng
        long totalOrders = orderRepository.count();

        // 3. Tổng số Shop đang hoạt động (Giả định Entity Shop có status="ACTIVE")
        long totalActiveShops = shopRepository.countByStatus("ACTIVE"); 
        
        // 4. Tổng doanh thu (Bạn cần thêm hàm tính tổng totalAmount vào OrderRepository)
        BigDecimal totalRevenue = orderRepository.sumTotalAmountIfStatusDelivered(); 
        
        // Tạo DTO phản hồi
        AdminDashboardStats stats = new AdminDashboardStats(
            totalUsers, 
            totalOrders, 
            totalActiveShops, 
            totalRevenue
        );

        return ResponseEntity.ok(stats);
    }
    
    // API 2: Lấy dữ liệu doanh thu theo tháng (Chart)
    @GetMapping("/revenue-chart")
    public ResponseEntity<Map<String, BigDecimal>> getMonthlyRevenue() {
        // Hàm này phức tạp hơn, cần truy vấn SQL Native hoặc dùng @Query.
        // Tạm thời trả về dữ liệu mẫu (Mock data)
        Map<String, BigDecimal> monthlyData = new HashMap<>();
        monthlyData.put("Tháng 8", new BigDecimal("15000000"));
        monthlyData.put("Tháng 9", new BigDecimal("22500000"));
        monthlyData.put("Tháng 10", new BigDecimal("31000000")); 
        
        return ResponseEntity.ok(monthlyData);
    }
}
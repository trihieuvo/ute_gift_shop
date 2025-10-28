package com.utegiftshop.controller;

import com.utegiftshop.dto.response.AdminDashboardStats;
import com.utegiftshop.repository.OrderRepository;
import com.utegiftshop.repository.ShopRepository;
import com.utegiftshop.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;

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

    // API 1: Lấy các số liệu thống kê tổng quan (ĐÃ THÊM BỘ LỌC)
    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardStats> getDashboardStats(
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate
    ) {
        Timestamp start = null;
        Timestamp end = null;
        
        try {
            if (startDate != null && !startDate.isEmpty()) {
                start = Timestamp.valueOf(startDate + " 00:00:00");
            }
            if (endDate != null && !endDate.isEmpty()) {
                end = Timestamp.valueOf(endDate + " 23:59:59");
            }
        } catch (IllegalArgumentException e) {
            // Xử lý lỗi ngày tháng (tạm thời bỏ qua và dùng null)
        }

        // 1. Tổng số User (Chỉ tính user mới trong khoảng thời gian)
        long totalNewUsers = userRepository.countByCreatedAtBetween(start, end);

        // 2. Tổng số Đơn hàng đã giao (Chỉ tính đơn hàng đã giao trong khoảng thời gian)
        long totalDeliveredOrders = orderRepository.countByStatusAndOrderDateBetween("DELIVERED", start, end);

        // 3. Tổng số Shop đang hoạt động (Không nên lọc theo ngày, lấy tổng số shop Active)
        long totalActiveShops = shopRepository.countByStatus("ACTIVE"); 
        
        // 4. Tổng doanh thu (Chỉ tính doanh thu đã giao trong khoảng thời gian)
        BigDecimal totalRevenue = orderRepository.sumTotalAmountIfStatusDeliveredAndOrderDateBetween(start, end);
        
        // Trả về DTO phản hồi
        AdminDashboardStats stats = new AdminDashboardStats(
            totalNewUsers, 
            totalDeliveredOrders, 
            totalActiveShops, 
            totalRevenue
        );

        return ResponseEntity.ok(stats);
    }
    
    // API 2: Lấy dữ liệu doanh thu theo tháng (Chart) - (ĐÃ THÊM BỘ LỌC)
    @GetMapping("/revenue-chart")
    public ResponseEntity<Map<String, BigDecimal>> getMonthlyRevenue(
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate
    ) {
        // Tùy chọn 1: Nếu bạn đã có hàm trong OrderRepository để tính doanh thu theo tháng/khoảng ngày, hãy dùng nó.
        // Tùy chọn 2: Nếu không, bạn cần viết logic để lấy dữ liệu.
        
        // Tạm thời trả về dữ liệu mẫu có thể lọc được (Mock data)
        // Trong thực tế, bạn phải truy vấn OrderRepository theo orderDate
        Map<String, BigDecimal> monthlyData = new HashMap<>();

        // Ví dụ: Lọc theo năm hiện tại nếu không có tham số
        if (startDate == null || endDate == null) {
             monthlyData.put("Tháng 8", new BigDecimal("15000000"));
             monthlyData.put("Tháng 9", new BigDecimal("22500000"));
             monthlyData.put("Tháng 10", new BigDecimal("31000000")); 
        } else {
             // Đây là logic giả định cho biểu đồ khi lọc (Cần code thật)
             monthlyData.put("Tháng 1", new BigDecimal("10000000"));
             monthlyData.put("Tháng 2", new BigDecimal("15000000"));
        }
        
        return ResponseEntity.ok(monthlyData);
    }
}
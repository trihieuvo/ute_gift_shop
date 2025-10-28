package com.utegiftshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class AdminDashboardStats {
    private long totalUsers;
    private long totalOrders;
    private long totalActiveShops;
    private BigDecimal totalRevenue;
}
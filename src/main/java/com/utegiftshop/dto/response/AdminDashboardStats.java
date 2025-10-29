package com.utegiftshop.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AdminDashboardStats {
    private long totalUsers;
    private long totalOrders;
    private long totalActiveShops;
    private BigDecimal totalRevenue;
    private BigDecimal totalCommissionRevenue;
}
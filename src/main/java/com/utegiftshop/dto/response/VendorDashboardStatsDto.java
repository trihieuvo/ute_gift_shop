package com.utegiftshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor // Default constructor
@AllArgsConstructor // Constructor with all arguments
public class VendorDashboardStatsDto {
    private long newOrdersCount;        // Count of new orders (status='NEW')
    private BigDecimal todayRevenue;    // Revenue for today (status='DELIVERED')
    private long lowStockCount;         // Count of products with low stock
}
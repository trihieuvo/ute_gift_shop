package com.utegiftshop.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id") // Null nếu là khuyến mãi của Admin
    private Shop shop;

    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @Column(name = "discount_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal discountPercent;

    @Column(name = "min_order_value", precision = 15, scale = 2)
    private BigDecimal minOrderValue;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "start_date", nullable = false)
    private Timestamp startDate;

    @Column(name = "end_date", nullable = false)
    private Timestamp endDate;
}
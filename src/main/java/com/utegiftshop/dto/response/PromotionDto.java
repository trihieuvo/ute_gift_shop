package com.utegiftshop.dto.response;

import com.utegiftshop.entity.Promotion;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Timestamp; // Sử dụng Timestamp giống Entity

@Getter
@Setter
@NoArgsConstructor
public class PromotionDto {
    private Integer id;
    private String code;
    private BigDecimal discountPercent;
    private BigDecimal minOrderValue;
    private Integer quantity; // Số lượng còn lại
    private Timestamp startDate; // Trả về Timestamp
    private Timestamp endDate;   // Trả về Timestamp

    public PromotionDto(Promotion promotion) {
        this.id = promotion.getId();
        this.code = promotion.getCode();
        this.discountPercent = promotion.getDiscountPercent();
        this.minOrderValue = promotion.getMinOrderValue();
        this.quantity = promotion.getQuantity();
        this.startDate = promotion.getStartDate();
        this.endDate = promotion.getEndDate();
    }
}
package com.utegiftshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewEligibilityDto {
    private boolean eligible;
    private Long orderDetailId; // ID của mục hàng trong đơn hàng để đánh giá
    private String message;
}
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
    private boolean eligible; // Đủ điều kiện viết MỚI hay không
    private Long orderDetailId; // ID của mục hàng trong đơn hàng để đánh giá (nếu eligible=true)
    private String message;
    private Long reviewId; // Thêm ID của đánh giá đã tồn tại (nếu eligible=false và đã đánh giá)
}

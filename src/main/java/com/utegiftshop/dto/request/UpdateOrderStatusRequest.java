package com.utegiftshop.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateOrderStatusRequest {
    private String newStatus;
    // Có thể thêm ghi chú nếu cần: private String note;
}
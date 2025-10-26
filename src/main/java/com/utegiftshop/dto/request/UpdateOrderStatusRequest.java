package com.utegiftshop.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateOrderStatusRequest {
    private String newStatus;
    
    // === BỔ SUNG: GHI CHÚ (LÝ DO THẤT BẠI) ===
    private String note;
    // === KẾT THÚC BỔ SUNG ===
}
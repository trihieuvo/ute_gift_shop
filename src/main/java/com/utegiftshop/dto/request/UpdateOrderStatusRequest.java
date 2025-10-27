package com.utegiftshop.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateOrderStatusRequest {
    private String newStatus;
    
    // === BỔ SUNG: GHI CHÚ (LÝ DO THẤT BẠI) ===
    private String note;
    
    // === BỔ SUNG: BẰNG CHỨNG GIAO HÀNG (POD) ===
    private String proofOfDeliveryImageUrl; 
    // === KẾT THÚC BỔ SUNG ===
}
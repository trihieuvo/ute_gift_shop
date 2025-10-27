package com.utegiftshop.dto.response;

import java.math.BigDecimal; // BỔ SUNG
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor 
public class ShipperStatsDto {
    private long assigned; // Đơn đang chờ/cần giao
    private long delivered; // Đơn đã giao thành công
    private long failed; // Đơn giao thất bại
    
    // === BỔ SUNG: TIỀN COD ĐANG GIỮ ===
    private BigDecimal totalCodHolding;
    // === KẾT THÚC BỔ SUNG ===
}
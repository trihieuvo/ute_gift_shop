package com.utegiftshop.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Timestamp; // Nhận Timestamp từ frontend (JavaScript sẽ gửi ISO string)

@Getter
@Setter
public class PromotionRequestDto {

    @NotBlank(message = "Mã KM không trống")
    @Size(min = 3, max = 50, message = "Mã KM từ 3-50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Mã KM chỉ chứa chữ cái và số")
    private String code;

    @NotNull(message = "% giảm giá không trống")
    @DecimalMin(value = "0.01", message = "% giảm giá > 0")
    @DecimalMax(value = "100.00", message = "% giảm giá <= 100")
    private BigDecimal discountPercent;

    @DecimalMin(value = "0.00", message = "Đơn tối thiểu không hợp lệ")
    private BigDecimal minOrderValue; // Có thể null

    @NotNull(message = "Số lượng không trống")
    @Min(value = 1, message = "Số lượng >= 1")
    private Integer quantity;

    @NotNull(message = "Ngày bắt đầu không trống")
    // @FutureOrPresent // Có thể gây lỗi nếu múi giờ client/server khác nhau, kiểm tra logic sau
    private Timestamp startDate;

    @NotNull(message = "Ngày kết thúc không trống")
    // @Future // Có thể gây lỗi nếu múi giờ khác, kiểm tra logic sau
    private Timestamp endDate;

    // Logic kiểm tra endDate > startDate sẽ thực hiện trong Controller
}
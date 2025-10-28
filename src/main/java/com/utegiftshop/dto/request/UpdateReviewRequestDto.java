package com.utegiftshop.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO dùng riêng cho việc cập nhật đánh giá đã có.
 * Chỉ chứa các trường có thể thay đổi.
 */
@Getter
@Setter
public class UpdateReviewRequestDto {

    @NotNull(message = "Vui lòng chọn số sao.")
    @Min(value = 1, message = "Đánh giá phải từ 1 đến 5 sao.")
    @Max(value = 5, message = "Đánh giá phải từ 1 đến 5 sao.")
    private Short rating;

    // Comment có thể là null hoặc rỗng khi cập nhật
    private String comment;
}

package com.utegiftshop.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequestDto {
    @NotNull
    private Long productId;

    @NotNull
    private Long orderDetailId;

    @NotNull(message = "Vui lòng chọn số sao.")
    @Min(value = 1, message = "Đánh giá phải từ 1 đến 5 sao.")
    @Max(value = 5, message = "Đánh giá phải từ 1 đến 5 sao.")
    private Short rating;

    private String comment;
}
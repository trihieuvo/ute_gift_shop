package com.utegiftshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorReviewReplyRequestDto {
    
    @NotBlank(message = "Nội dung phản hồi không được để trống")
    @Size(min = 1, max = 1000, message = "Nội dung phản hồi phải từ 1 đến 1000 ký tự")
    private String replyContent;
}
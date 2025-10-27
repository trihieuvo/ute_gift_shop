package com.utegiftshop.dto.response;

import java.time.format.DateTimeFormatter;

import com.utegiftshop.entity.Review;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewDto {
    private String userFullName;
    private Short rating;
    private String comment;
    private String createdAt;

    public ReviewDto(Review review) {
        this.userFullName = review.getUser() != null ? review.getUser().getFullName() : "Người dùng ẩn danh";
        this.rating = review.getRating();
        this.comment = review.getComment();
        // Format ngày tháng cho đẹp hơn
        if (review.getCreatedAt() != null) {
            this.createdAt = review.getCreatedAt().toLocalDateTime().format(DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy"));
        }
    }
}
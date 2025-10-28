package com.utegiftshop.dto.response;

import java.time.format.DateTimeFormatter;
import java.sql.Timestamp; // Sử dụng Timestamp để nhất quán

import com.utegiftshop.entity.Review;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewDto {
    private Long id; // Thêm ID của đánh giá
    private Long userId; // Thêm ID của người dùng
    private String userFullName;
    private Short rating;
    private String comment;
    private String createdAt;
    private String updatedAt; // Thêm thời gian cập nhật
    private String vendorReply; // Thêm phản hồi của vendor
    private String repliedAtFormatted; // Thêm thời gian vendor phản hồi

    // Định dạng ngày giờ
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy");

    public ReviewDto(Review review) {
        this.id = review.getId(); // Lấy ID
        this.userFullName = review.getUser() != null ? review.getUser().getFullName() : "Người dùng ẩn danh";
        this.userId = review.getUser() != null ? review.getUser().getId() : null; // Lấy userId
        this.rating = review.getRating();
        this.comment = review.getComment();
        this.vendorReply = review.getVendorReply(); // Lấy phản hồi

        // Format ngày tạo
        if (review.getCreatedAt() != null) {
            this.createdAt = review.getCreatedAt().toLocalDateTime().format(formatter);
        } else {
            this.createdAt = "N/A";
        }

        // Format ngày cập nhật (nếu có)
        if (review.getUpdatedAt() != null) {
            this.updatedAt = review.getUpdatedAt().toLocalDateTime().format(formatter);
        } else {
            this.updatedAt = null;
        }

        // Format ngày vendor phản hồi (nếu có)
        if (review.getRepliedAt() != null) {
            this.repliedAtFormatted = review.getRepliedAt().toLocalDateTime().format(formatter);
        } else {
            this.repliedAtFormatted = null;
        }
    }
}


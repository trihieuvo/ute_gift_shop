package com.utegiftshop.dto.response;

import com.utegiftshop.entity.Review;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp; // Or java.time.LocalDateTime if you prefer and format later
import java.time.format.DateTimeFormatter; // If formatting here

@Getter
@Setter
@NoArgsConstructor
public class VendorReviewDto {
    private Long reviewId;
    private Long productId;
    private String productName;
    private String customerName; // User who wrote the review
    private Short rating;
    private String comment;
    private String createdAtFormatted; // Formatted timestamp

    // --- NEW FIELDS ---
    private String vendorReply;
    private String repliedAtFormatted;
    // --- END NEW FIELDS ---

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy");

    public VendorReviewDto(Review review) {
        this.reviewId = review.getId();
        this.rating = review.getRating();
        this.comment = review.getComment();

        if (review.getProduct() != null) {
            this.productId = review.getProduct().getId();
            this.productName = review.getProduct().getName();
        } else {
            this.productName = "Sản phẩm không xác định";
        }

        if (review.getUser() != null) {
            this.customerName = review.getUser().getFullName();
        } else {
            this.customerName = "Khách hàng ẩn danh";
        }

        if (review.getCreatedAt() != null) {
            this.createdAtFormatted = review.getCreatedAt().toLocalDateTime().format(formatter);
        } else {
            this.createdAtFormatted = "N/A";
        }

        // --- POPULATE NEW FIELDS ---
        this.vendorReply = review.getVendorReply();
        if (review.getRepliedAt() != null) {
            this.repliedAtFormatted = review.getRepliedAt().toLocalDateTime().format(formatter);
        } else {
            this.repliedAtFormatted = null; // No reply yet
        }
        // --- END POPULATE ---
    }
}
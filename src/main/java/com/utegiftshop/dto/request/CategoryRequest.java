package com.utegiftshop.dto.request;

import jakarta.validation.constraints.NotBlank; // Thư viện validation
import lombok.Data; // Bao gồm @Getter, @Setter, @ToString, etc.

@Data // Sử dụng @Data cho gọn
public class CategoryRequest {

    @NotBlank(message = "Tên danh mục không được để trống") // Thêm validation
    private String name;

    private Integer parentId; // Giữ lại trường này (nullable) để xử lý danh mục cha-con nếu cần

    // Không cần id, không cần children trong Request DTO
}
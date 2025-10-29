package com.utegiftshop.dto.response;

import com.utegiftshop.entity.Category;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class CategoryBasicDto {
    private Integer id;
    private String name;

    public CategoryBasicDto(Category cat) {
        if (cat != null) {
            this.id = cat.getId();
            this.name = cat.getName();
        }
    }
}
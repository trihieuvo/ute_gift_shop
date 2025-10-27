package com.utegiftshop.dto.request;

import java.util.ArrayList;
import java.util.List;

import com.utegiftshop.entity.Category;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CategoryDto {
    private Integer id;
    private String name;
    private List<CategoryDto> children = new ArrayList<>();

    public CategoryDto(Category category) {
        this.id = category.getId();
        this.name = category.getName();
    }
}
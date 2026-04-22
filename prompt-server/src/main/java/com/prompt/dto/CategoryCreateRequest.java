package com.prompt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryCreateRequest {
    @NotBlank(message = "分类名称不能为空")
    private String name;
    private Long parentId;
    private Integer sortOrder;
    private String icon;
    private String color;
}

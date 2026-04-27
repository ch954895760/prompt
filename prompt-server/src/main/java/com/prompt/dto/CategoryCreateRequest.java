package com.prompt.dto;

import java.util.List;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryCreateRequest {
    @NotBlank(message = "分类名称不能为空")
    private String name;
    private Long parentId;
    private Integer sortOrder;
    private String icon;
    private String color;

    @Data
    public static class SortItem {
        private Long id;
        private Integer sortOrder;
        private Long parentId;
    }

    @Data
    public static class SortRequest {
        private List<SortItem> items;
    }
}


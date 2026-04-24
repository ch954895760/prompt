package com.prompt.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class PromptCreateRequest {
    @NotBlank(message = "标题不能为空")
    private String title;
    @NotBlank(message = "内容不能为空")
    private String content;
    private String description;
    private Long categoryId;
    private String variablesJson;
    private Boolean isPublic;
    private List<Long> tagIds;
}


package com.prompt.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TagCreateRequest {
    @NotBlank(message = "标签名称不能为空")
    private String name;
    private String color;
}


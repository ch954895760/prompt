package com.prompt.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class AiTestRequest {
    @NotBlank(message = "提示词内容不能为空")
    private String content;

    private Long providerId;
}

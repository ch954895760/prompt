package com.prompt.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class AiProviderUpdateRequest {
    @NotBlank(message = "名称不能为空")
    private String name;

    @NotBlank(message = "提供商不能为空")
    private String provider;

    @NotBlank(message = "API Base URL不能为空")
    private String apiBaseUrl;

    private String apiKey;

    @NotBlank(message = "模型不能为空")
    private String model;

    @NotNull(message = "是否默认不能为空")
    private Boolean isDefault;
}

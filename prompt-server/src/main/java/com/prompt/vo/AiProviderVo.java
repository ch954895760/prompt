package com.prompt.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiProviderVo {
    private Long id;
    private String name;
    private String provider;
    private String apiBaseUrl;
    private String model;
    private Boolean isDefault;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.prompt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_provider")
public class AiProvider {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String provider;
    private String apiBaseUrl;
    private String apiKeyEncrypted;
    private String model;
    private Boolean isDefault;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

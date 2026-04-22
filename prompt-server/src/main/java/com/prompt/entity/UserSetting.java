package com.prompt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_setting")
public class UserSetting {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String theme;
    private String defaultModel;
    private String apiBaseUrl;
    private String apiKeyEncrypted;
    private String model;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

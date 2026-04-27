package com.prompt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("prompt_usage_log")
public class PromptUsageLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long promptId;
    private String context;
    private LocalDateTime createdAt;
}

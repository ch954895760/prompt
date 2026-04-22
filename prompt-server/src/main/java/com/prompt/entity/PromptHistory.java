package com.prompt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("prompt_history")
public class PromptHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long promptId;
    private String content;
    private Integer version;
    private LocalDateTime createdAt;
}

package com.prompt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("category")
public class Category {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private Long parentId;
    private Integer sortOrder;
    private String icon;
    private String color;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private List<Category> children;

    @TableField(exist = false)
    private Integer promptCount;
}

package com.prompt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.prompt.entity.PromptUsageLog;
import com.prompt.entity.Prompt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PromptUsageLogMapper extends BaseMapper<PromptUsageLog> {

    @Select("SELECT p.*, c.name as category_name, c.color as category_color, MAX(l.created_at) as last_used_at " +
            "FROM prompt_usage_log l " +
            "JOIN prompt p ON l.prompt_id = p.id " +
            "LEFT JOIN category c ON p.category_id = c.id " +
            "WHERE l.user_id = #{userId} " +
            "GROUP BY l.prompt_id, p.id, c.name, c.color " +
            "ORDER BY last_used_at DESC " +
            "LIMIT #{limit}")
    List<Prompt> selectRecentlyUsed(@Param("userId") Long userId, @Param("limit") Integer limit);
}

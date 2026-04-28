package com.prompt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.prompt.entity.AiProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AiProviderMapper extends BaseMapper<AiProvider> {

    @Select("SELECT * FROM ai_provider WHERE user_id = #{userId} ORDER BY sort_order ASC, created_at ASC")
    List<AiProvider> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM ai_provider WHERE user_id = #{userId} AND is_default = 1 LIMIT 1")
    AiProvider selectDefaultByUserId(@Param("userId") Long userId);

    @Update("UPDATE ai_provider SET is_default = 0 WHERE user_id = #{userId}")
    int clearDefaultByUserId(@Param("userId") Long userId);

    @Select("SELECT MAX(sort_order) FROM ai_provider WHERE user_id = #{userId}")
    Integer selectMaxSortOrderByUserId(@Param("userId") Long userId);

    /**
     * 查询系统级公共模型（user_id为NULL）
     */
    @Select("SELECT * FROM ai_provider WHERE user_id IS NULL ORDER BY sort_order ASC, created_at ASC")
    List<AiProvider> selectSystemProviders();

    /**
     * 查询系统默认模型
     */
    @Select("SELECT * FROM ai_provider WHERE user_id IS NULL AND is_default = 1 LIMIT 1")
    AiProvider selectSystemDefault();
}

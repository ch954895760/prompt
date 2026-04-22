package com.prompt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.prompt.entity.Prompt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PromptMapper extends BaseMapper<Prompt> {
    @Select("SELECT p.*, c.name as category_name FROM prompt p LEFT JOIN category c ON p.category_id = c.id WHERE p.user_id = #{userId} ORDER BY p.updated_at DESC")
    List<Prompt> selectListWithCategory(@Param("userId") Long userId);

    @Select("SELECT p.*, c.name as category_name FROM prompt p LEFT JOIN category c ON p.category_id = c.id WHERE p.id = #{id}")
    Prompt selectByIdWithCategory(@Param("id") Long id);
}

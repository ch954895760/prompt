package com.prompt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.prompt.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
    @Select("SELECT c.*, COUNT(p.id) as prompt_count FROM category c LEFT JOIN prompt p ON c.id = p.category_id WHERE c.user_id = #{userId} GROUP BY c.id ORDER BY c.sort_order")
    List<Category> selectListWithCount(@Param("userId") Long userId);
}

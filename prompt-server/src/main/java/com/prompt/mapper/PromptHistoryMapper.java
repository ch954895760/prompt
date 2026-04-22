package com.prompt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.prompt.entity.PromptHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PromptHistoryMapper extends BaseMapper<PromptHistory> {

    @Select("SELECT * FROM prompt_history WHERE prompt_id = #{promptId} ORDER BY version DESC")
    List<PromptHistory> selectByPromptId(@Param("promptId") Long promptId);

    @Select("SELECT * FROM prompt_history WHERE prompt_id = #{promptId} AND version = #{version} LIMIT 1")
    PromptHistory selectByPromptIdAndVersion(@Param("promptId") Long promptId, @Param("version") Integer version);
}

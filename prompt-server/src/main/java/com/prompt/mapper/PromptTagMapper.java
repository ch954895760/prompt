package com.prompt.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PromptTagMapper {

    @Insert("INSERT INTO prompt_tag (prompt_id, tag_id) VALUES (#{promptId}, #{tagId})")
    void insert(@Param("promptId") Long promptId, @Param("tagId") Long tagId);

    @Delete("DELETE FROM prompt_tag WHERE prompt_id = #{promptId}")
    void deleteByPromptId(@Param("promptId") Long promptId);

    @Select("SELECT tag_id FROM prompt_tag WHERE prompt_id = #{promptId}")
    List<Long> selectTagIdsByPromptId(@Param("promptId") Long promptId);

    @Select("SELECT prompt_id FROM prompt_tag WHERE tag_id = #{tagId}")
    List<Long> selectPromptIdsByTagId(@Param("tagId") Long tagId);
}

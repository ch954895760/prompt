package com.prompt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.prompt.entity.UserSetting;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserSettingMapper extends BaseMapper<UserSetting> {
}

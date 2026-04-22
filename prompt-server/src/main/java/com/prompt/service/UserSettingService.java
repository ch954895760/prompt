package com.prompt.service;

import com.prompt.dto.UserSettingUpdateRequest;
import com.prompt.entity.UserSetting;
import com.prompt.exception.BusinessException;
import com.prompt.mapper.UserSettingMapper;
import com.prompt.util.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSettingService {

    private final UserSettingMapper userSettingMapper;
    private final AesUtil aesUtil;

    public UserSetting getByUserId(Long userId) {
        UserSetting setting = userSettingMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserSetting>()
                        .eq(UserSetting::getUserId, userId)
        );
        if (setting == null) {
            setting = new UserSetting();
            setting.setUserId(userId);
            setting.setTheme("light");
            userSettingMapper.insert(setting);
        }
        return setting;
    }

    public UserSetting update(Long userId, UserSettingUpdateRequest request) {
        UserSetting setting = getByUserId(userId);
        if (request.getTheme() != null) {
            setting.setTheme(request.getTheme());
        }
        if (request.getDefaultModel() != null) {
            setting.setDefaultModel(request.getDefaultModel());
        }
        if (request.getApiBaseUrl() != null) {
            setting.setApiBaseUrl(request.getApiBaseUrl());
        }
        if (request.getApiKey() != null && !request.getApiKey().isEmpty()) {
            try {
                setting.setApiKeyEncrypted(aesUtil.encrypt(request.getApiKey()));
            } catch (Exception e) {
                log.error("[DEBUG] Failed to encrypt API key: {}", e.getMessage());
                throw new BusinessException("API Key 加密失败");
            }
        }
        if (request.getModel() != null) {
            setting.setModel(request.getModel());
        }
        userSettingMapper.updateById(setting);
        return setting;
    }
}

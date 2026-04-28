package com.prompt.service;

import com.prompt.dto.AiProviderCreateRequest;
import com.prompt.dto.AiProviderUpdateRequest;
import com.prompt.entity.AiProvider;
import com.prompt.exception.BusinessException;
import com.prompt.mapper.AiProviderMapper;
import com.prompt.util.AesUtil;
import com.prompt.vo.AiProviderVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiProviderService {

    private final AiProviderMapper aiProviderMapper;
    private final AesUtil aesUtil;

    /**
     * 查询用户的所有AI模型（包括系统级公共模型）
     */
    public List<AiProviderVo> listByUserId(Long userId) {
        List<AiProvider> userProviders = aiProviderMapper.selectByUserId(userId);
        List<AiProvider> systemProviders = aiProviderMapper.selectSystemProviders();

        List<AiProvider> allProviders = new ArrayList<>();
        allProviders.addAll(systemProviders);
        allProviders.addAll(userProviders);

        return allProviders.stream().map(this::convertToVo).collect(Collectors.toList());
    }

    /**
     * 仅查询系统级公共模型
     */
    public List<AiProviderVo> listSystemProviders() {
        List<AiProvider> systemProviders = aiProviderMapper.selectSystemProviders();
        return systemProviders.stream().map(this::convertToVo).collect(Collectors.toList());
    }

    public AiProviderVo getById(Long id, Long userId) {
        AiProvider provider = aiProviderMapper.selectById(id);
        if (provider == null) {
            throw new BusinessException("AI提供商不存在");
        }
        // 系统模型对所有用户可见，用户模型只能所有者查看
        if (provider.getUserId() != null && !provider.getUserId().equals(userId)) {
            throw new BusinessException("AI提供商不存在");
        }
        return convertToVo(provider);
    }

    public AiProvider getEntityById(Long id, Long userId) {
        AiProvider provider = aiProviderMapper.selectById(id);
        if (provider == null) {
            throw new BusinessException("AI提供商不存在");
        }
        // 系统模型对所有用户可见，用户模型只能所有者查看
        if (provider.getUserId() != null && !provider.getUserId().equals(userId)) {
            throw new BusinessException("AI提供商不存在");
        }
        return provider;
    }

    public AiProvider getDefaultByUserId(Long userId) {
        // 1. 先查找用户设置的默认模型
        AiProvider provider = aiProviderMapper.selectDefaultByUserId(userId);
        if (provider != null) {
            return provider;
        }

        // 2. 查找用户的第一个模型
        List<AiProvider> providers = aiProviderMapper.selectByUserId(userId);
        if (!providers.isEmpty()) {
            return providers.get(0);
        }

        // 3. 使用系统默认模型
        return aiProviderMapper.selectSystemDefault();
    }

    /**
     * 获取默认的系统AI配置
     */
    public AiProvider getSystemDefaultProvider() {
        return aiProviderMapper.selectSystemDefault();
    }

    /**
     * 检查是否配置了系统默认模型
     */
    public boolean hasSystemDefaultProvider() {
        return aiProviderMapper.selectSystemDefault() != null;
    }

    @Transactional
    public AiProviderVo create(Long userId, AiProviderCreateRequest request) {
        AiProvider provider = new AiProvider();
        provider.setUserId(userId);
        provider.setName(request.getName());
        provider.setProvider(request.getProvider());
        provider.setApiBaseUrl(request.getApiBaseUrl());
        provider.setModel(request.getModel());
        provider.setIsDefault(request.getIsDefault());

        Integer maxSortOrder = aiProviderMapper.selectMaxSortOrderByUserId(userId);
        provider.setSortOrder(maxSortOrder == null ? 0 : maxSortOrder + 1);

        try {
            String encryptedKey = aesUtil.encrypt(request.getApiKey());
            provider.setApiKeyEncrypted(encryptedKey);
        } catch (Exception e) {
            log.error("[DEBUG] Failed to encrypt API key: {}", e.getMessage());
            throw new BusinessException("API Key加密失败");
        }

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            aiProviderMapper.clearDefaultByUserId(userId);
        }

        aiProviderMapper.insert(provider);
        return convertToVo(provider);
    }

    @Transactional
    public AiProviderVo update(Long id, Long userId, AiProviderUpdateRequest request) {
        AiProvider provider = aiProviderMapper.selectById(id);
        if (provider == null) {
            throw new BusinessException("AI提供商不存在");
        }

        // 系统模型不允许普通用户修改
        if (provider.getUserId() == null) {
            throw new BusinessException("系统模型不能修改");
        }

        // 只能修改自己的模型
        if (!provider.getUserId().equals(userId)) {
            throw new BusinessException("AI提供商不存在");
        }

        provider.setName(request.getName());
        provider.setProvider(request.getProvider());
        provider.setApiBaseUrl(request.getApiBaseUrl());
        provider.setModel(request.getModel());

        if (request.getApiKey() != null && !request.getApiKey().isEmpty()) {
            try {
                String encryptedKey = aesUtil.encrypt(request.getApiKey());
                provider.setApiKeyEncrypted(encryptedKey);
            } catch (Exception e) {
                log.error("[DEBUG] Failed to encrypt API key: {}", e.getMessage());
                throw new BusinessException("API Key加密失败");
            }
        }

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(provider.getIsDefault())) {
            aiProviderMapper.clearDefaultByUserId(userId);
            provider.setIsDefault(true);
        } else {
            provider.setIsDefault(request.getIsDefault());
        }

        aiProviderMapper.updateById(provider);
        return convertToVo(provider);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        AiProvider provider = aiProviderMapper.selectById(id);
        if (provider == null) {
            throw new BusinessException("AI提供商不存在");
        }

        // 系统模型不允许删除
        if (provider.getUserId() == null) {
            throw new BusinessException("系统模型不能删除");
        }

        // 只能删除自己的模型
        if (!provider.getUserId().equals(userId)) {
            throw new BusinessException("AI提供商不存在");
        }

        aiProviderMapper.deleteById(id);
    }

    @Transactional
    public void setDefault(Long id, Long userId) {
        AiProvider provider = aiProviderMapper.selectById(id);
        if (provider == null) {
            throw new BusinessException("AI提供商不存在");
        }

        // 系统模型不能设为用户的默认模型
        if (provider.getUserId() == null) {
            throw new BusinessException("系统模型不能设为默认");
        }

        // 只能设置自己的模型为默认
        if (!provider.getUserId().equals(userId)) {
            throw new BusinessException("AI提供商不存在");
        }

        aiProviderMapper.clearDefaultByUserId(userId);
        provider.setIsDefault(true);
        aiProviderMapper.updateById(provider);
    }

    private AiProviderVo convertToVo(AiProvider provider) {
        AiProviderVo vo = new AiProviderVo();
        BeanUtils.copyProperties(provider, vo);
        // 设置是否为系统模型标识
        vo.setIsSystem(provider.getUserId() == null);
        return vo;
    }
}

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiProviderService {

    private final AiProviderMapper aiProviderMapper;
    private final AesUtil aesUtil;

    @Value("${ai.default.enabled:false}")
    private Boolean defaultAiEnabled;

    @Value("${ai.default.base-url:}")
    private String defaultBaseUrl;

    @Value("${ai.default.api-key:}")
    private String defaultApiKey;

    @Value("${ai.default.model:}")
    private String defaultModel;

    public List<AiProviderVo> listByUserId(Long userId) {
        List<AiProvider> providers = aiProviderMapper.selectByUserId(userId);
        return providers.stream().map(this::convertToVo).collect(Collectors.toList());
    }

    public AiProviderVo getById(Long id, Long userId) {
        AiProvider provider = aiProviderMapper.selectById(id);
        if (provider == null || !provider.getUserId().equals(userId)) {
            throw new BusinessException("AI提供商不存在");
        }
        return convertToVo(provider);
    }

    public AiProvider getEntityById(Long id, Long userId) {
        AiProvider provider = aiProviderMapper.selectById(id);
        if (provider == null || !provider.getUserId().equals(userId)) {
            throw new BusinessException("AI提供商不存在");
        }
        return provider;
    }

    public AiProvider getDefaultByUserId(Long userId) {
        AiProvider provider = aiProviderMapper.selectDefaultByUserId(userId);
        if (provider == null) {
            List<AiProvider> providers = aiProviderMapper.selectByUserId(userId);
            if (!providers.isEmpty()) {
                provider = providers.get(0);
            }
        }
        // 如果用户没有配置模型，且启用了默认模型，则返回默认模型
        if (provider == null && Boolean.TRUE.equals(defaultAiEnabled)) {
            provider = createDefaultProvider();
        }
        return provider;
    }

    /**
     * 获取默认的系统AI配置（不与任何用户关联）
     * 当用户查询不到自己的模型时，可以使用该默认模型
     */
    public AiProvider getSystemDefaultProvider() {
        if (Boolean.TRUE.equals(defaultAiEnabled)) {
            return createDefaultProvider();
        }
        return null;
    }

    /**
     * 创建默认的AI提供商实体
     */
    private AiProvider createDefaultProvider() {
        AiProvider provider = new AiProvider();
        provider.setId(-1L); // 使用负数ID标识系统默认模型
        provider.setUserId(-1L); // 不与任何用户关联
        provider.setName("系统默认模型");
        provider.setProvider("default");
        provider.setApiBaseUrl(defaultBaseUrl);
        provider.setApiKeyEncrypted(defaultApiKey);
        provider.setModel(defaultModel);
        provider.setIsDefault(true);
        provider.setSortOrder(0);
        return provider;
    }

    /**
     * 检查是否配置了系统默认模型
     */
    public boolean hasSystemDefaultProvider() {
        return Boolean.TRUE.equals(defaultAiEnabled) 
               && defaultBaseUrl != null && !defaultBaseUrl.isEmpty()
               && defaultApiKey != null && !defaultApiKey.isEmpty()
               && defaultModel != null && !defaultModel.isEmpty();
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
        if (provider == null || !provider.getUserId().equals(userId)) {
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
        if (provider == null || !provider.getUserId().equals(userId)) {
            throw new BusinessException("AI提供商不存在");
        }
        aiProviderMapper.deleteById(id);
    }

    @Transactional
    public void setDefault(Long id, Long userId) {
        AiProvider provider = aiProviderMapper.selectById(id);
        if (provider == null || !provider.getUserId().equals(userId)) {
            throw new BusinessException("AI提供商不存在");
        }
        aiProviderMapper.clearDefaultByUserId(userId);
        provider.setIsDefault(true);
        aiProviderMapper.updateById(provider);
    }

    private AiProviderVo convertToVo(AiProvider provider) {
        AiProviderVo vo = new AiProviderVo();
        BeanUtils.copyProperties(provider, vo);
        return vo;
    }
}

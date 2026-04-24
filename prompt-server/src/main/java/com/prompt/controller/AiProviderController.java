package com.prompt.controller;

import com.prompt.dto.AiProviderCreateRequest;
import com.prompt.dto.AiProviderUpdateRequest;
import com.prompt.entity.AiProvider;
import com.prompt.service.AiProviderService;
import com.prompt.vo.AiProviderVo;
import com.prompt.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/ai-providers")
@RequiredArgsConstructor
public class AiProviderController {

    private final AiProviderService aiProviderService;

    private Long getCurrentUserId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }

    @GetMapping
    public Result<List<AiProviderVo>> list(Authentication authentication) {
        return Result.success(aiProviderService.listByUserId(getCurrentUserId(authentication)));
    }

    @GetMapping("/{id}")
    public Result<AiProviderVo> get(@PathVariable Long id, Authentication authentication) {
        return Result.success(aiProviderService.getById(id, getCurrentUserId(authentication)));
    }

    @GetMapping("/default")
    public Result<AiProviderVo> getDefault(Authentication authentication) {
        AiProvider provider = aiProviderService.getDefaultByUserId(getCurrentUserId(authentication));
        if (provider == null) {
            return Result.success(null);
        }
        AiProviderVo vo = new AiProviderVo();
        vo.setId(provider.getId());
        vo.setName(provider.getName());
        vo.setProvider(provider.getProvider());
        vo.setApiBaseUrl(provider.getApiBaseUrl());
        vo.setModel(provider.getModel());
        vo.setIsDefault(provider.getIsDefault());
        vo.setSortOrder(provider.getSortOrder());
        vo.setCreatedAt(provider.getCreatedAt());
        vo.setUpdatedAt(provider.getUpdatedAt());
        return Result.success(vo);
    }

    @PostMapping
    public Result<AiProviderVo> create(@Valid @RequestBody AiProviderCreateRequest request, Authentication authentication) {
        return Result.success(aiProviderService.create(getCurrentUserId(authentication), request));
    }

    @PutMapping("/{id}")
    public Result<AiProviderVo> update(@PathVariable Long id, @Valid @RequestBody AiProviderUpdateRequest request, Authentication authentication) {
        return Result.success(aiProviderService.update(id, getCurrentUserId(authentication), request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication authentication) {
        aiProviderService.delete(id, getCurrentUserId(authentication));
        return Result.success();
    }

    @PostMapping("/{id}/set-default")
    public Result<Void> setDefault(@PathVariable Long id, Authentication authentication) {
        aiProviderService.setDefault(id, getCurrentUserId(authentication));
        return Result.success();
    }
}

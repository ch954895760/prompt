package com.prompt.controller;

import com.prompt.dto.PromptOptimizeRequest;
import com.prompt.dto.PromptOptimizeResponse;
import com.prompt.service.PromptOptimizerService;
import com.prompt.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/prompts")
@RequiredArgsConstructor
public class PromptOptimizerController {

    private final PromptOptimizerService optimizerService;

    @PostMapping("/optimize")
    public Result<PromptOptimizeResponse> optimize(
            @RequestBody @Valid PromptOptimizeRequest request,
            Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        log.debug("[DEBUG] Prompt optimize request from user: {}", userId);
        return Result.success(optimizerService.optimize(userId, request));
    }

    @DeleteMapping("/optimize/cache")
    public Result<Void> clearCache(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        optimizerService.clearUserCache(userId);
        log.debug("[DEBUG] Cache cleared for user: {}", userId);
        return Result.success();
    }
}

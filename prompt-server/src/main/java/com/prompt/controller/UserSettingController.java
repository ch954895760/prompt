package com.prompt.controller;

import com.prompt.dto.AiTestRequest;
import com.prompt.dto.UserSettingUpdateRequest;
import com.prompt.entity.UserSetting;
import com.prompt.service.UserSettingService;
import com.prompt.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class UserSettingController {

    private final UserSettingService userSettingService;

    private Long getCurrentUserId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }

    @GetMapping
    public Result<UserSetting> get(Authentication authentication) {
        return Result.success(userSettingService.getByUserId(getCurrentUserId(authentication)));
    }

    @PutMapping
    public Result<UserSetting> update(@RequestBody UserSettingUpdateRequest request, Authentication authentication) {
        return Result.success(userSettingService.update(getCurrentUserId(authentication), request));
    }

    @PostMapping("/ai-test")
    public Result<String> aiTest(@RequestBody AiTestRequest request, Authentication authentication) {
        return Result.success("AI 测试接口已收到请求。请配置 base_url 和 api_key 后使用。");
    }
}

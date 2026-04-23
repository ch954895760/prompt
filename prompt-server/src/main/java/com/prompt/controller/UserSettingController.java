package com.prompt.controller;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.prompt.dto.AiTestRequest;
import com.prompt.dto.UserSettingUpdateRequest;
import com.prompt.entity.UserSetting;
import com.prompt.exception.BusinessException;
import com.prompt.service.UserSettingService;
import com.prompt.util.AesUtil;
import com.prompt.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class UserSettingController {

    private final UserSettingService userSettingService;
    private final AesUtil aesUtil;

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
        Long userId = getCurrentUserId(authentication);
        UserSetting setting = userSettingService.getByUserId(userId);

        if (setting.getApiBaseUrl() == null || setting.getApiBaseUrl().isEmpty()) {
            throw new BusinessException("AI 测试功能需要配置 base_url。请在设置中配置后使用。");
        }
        if (setting.getApiKeyEncrypted() == null || setting.getApiKeyEncrypted().isEmpty()) {
            throw new BusinessException("AI 测试功能需要配置 api_key。请在设置中配置后使用。");
        }

        String apiKey;
        try {
            apiKey = aesUtil.decrypt(setting.getApiKeyEncrypted());
        } catch (Exception e) {
            log.error("[DEBUG] Failed to decrypt API key: {}", e.getMessage());
            throw new BusinessException("API Key 解密失败，请重新配置");
        }
        System.out.println("apiKey = " + apiKey);

        String baseUrl = setting.getApiBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String model = setting.getModel();
        if (model == null || model.isEmpty()) {
            model = "gpt-4.1-mini";
        }

        try {
            OpenAIClient client = OpenAIOkHttpClient.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .build();

            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .addUserMessage(request.getContent())
                    .model(ChatModel.of(model))
                    .build();

            ChatCompletion chatCompletion = client.chat().completions().create(params);
            String content = chatCompletion.choices().get(0).message().content().orElseThrow(
                    () -> new BusinessException("AI 返回空内容")
            );
            return Result.success(content);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[DEBUG] AI test request failed: {}", e.getMessage());
            throw new BusinessException("AI 请求失败，请检查 API Key、模型名称和 Base URL 是否正确");
        }
    }
}

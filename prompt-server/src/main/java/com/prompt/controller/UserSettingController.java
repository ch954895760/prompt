package com.prompt.controller;

import com.prompt.dto.AiTestRequest;
import com.prompt.dto.UserSettingUpdateRequest;
import com.prompt.entity.UserSetting;
import com.prompt.exception.BusinessException;
import com.prompt.service.UserSettingService;
import com.prompt.util.AesUtil;
import com.prompt.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class UserSettingController {

    private final UserSettingService userSettingService;
    private final AesUtil aesUtil;
    private final RestTemplate restTemplate = createRestTemplate();

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(25000);
        factory.setReadTimeout(25000);
        return new RestTemplate(factory);
    }

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

        String baseUrl = setting.getApiBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String url = baseUrl + "/v1/chat/completions";

        String model = setting.getModel();
        if (model == null || model.isEmpty()) {
            model = "gpt-3.5-turbo";
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", request.getContent()))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new BusinessException("AI 返回空响应");
            }
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new BusinessException("AI 返回无效响应");
            }
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            return Result.success(content);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[DEBUG] AI test request failed: {}", e.getMessage());
            throw new BusinessException("AI 请求失败: " + e.getMessage());
        }
    }
}

package com.prompt.controller;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.prompt.dto.AiTestRequest;
import com.prompt.dto.UserSettingUpdateRequest;
import com.prompt.entity.AiProvider;
import com.prompt.entity.UserSetting;
import com.prompt.exception.BusinessException;
import com.prompt.service.AiProviderService;
import com.prompt.service.UserSettingService;
import com.prompt.util.AesUtil;
import com.prompt.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class UserSettingController {

    private final UserSettingService userSettingService;
    private final AiProviderService aiProviderService;
    private final AesUtil aesUtil;

    private ExecutorService sseExecutor;

    @PostConstruct
    public void init() {
        sseExecutor = Executors.newCachedThreadPool();
    }

    @PreDestroy
    public void destroy() {
        if (sseExecutor != null) {
            sseExecutor.shutdown();
        }
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

    @PostMapping(value = "/ai-test", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter aiTest(@RequestBody AiTestRequest request, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);

        String baseUrl;
        String apiKey;
        String model;
        boolean isSystemDefault;

        if (request.getProviderId() != null) {
            AiProvider provider = aiProviderService.getEntityById(request.getProviderId(), userId);
            if (provider == null) {
                throw new BusinessException("AI提供商不存在");
            }
            baseUrl = provider.getApiBaseUrl();
            model = provider.getModel();
            // 检查是否为系统默认模型（ID为负数表示系统默认）
            isSystemDefault = provider.getId() != null && provider.getId() < 0;
            try {
                if (isSystemDefault) {
                    // 系统默认模型的API Key是明文存储的，不需要解密
                    apiKey = provider.getApiKeyEncrypted();
                } else {
                    // 用户配置的模型需要解密
                    apiKey = aesUtil.decrypt(provider.getApiKeyEncrypted());
                }
            } catch (Exception e) {
                log.error("[DEBUG] Failed to decrypt API key: {}", e.getMessage());
                throw new BusinessException("API Key 解密失败，请重新配置");
            }
        } else {
            // 优先尝试获取用户配置的默认AI提供商
            AiProvider provider = aiProviderService.getDefaultByUserId(userId);
            if (provider != null) {
                baseUrl = provider.getApiBaseUrl();
                model = provider.getModel();
                // 检查是否为系统默认模型（ID为负数表示系统默认）
                isSystemDefault = provider.getId() != null && provider.getId() < 0;
                try {
                    if (isSystemDefault) {
                        // 系统默认模型的API Key是明文存储的，不需要解密
                        apiKey = provider.getApiKeyEncrypted();
                    } else {
                        // 用户配置的模型需要解密
                        apiKey = aesUtil.decrypt(provider.getApiKeyEncrypted());
                    }
                } catch (Exception e) {
                    log.error("[DEBUG] Failed to decrypt API key: {}", e.getMessage());
                    throw new BusinessException("API Key 解密失败，请重新配置");
                }
            } else {
                // 如果没有配置AI提供商，尝试使用用户设置
                UserSetting setting = userSettingService.getByUserId(userId);
                if (setting.getApiBaseUrl() == null || setting.getApiBaseUrl().isEmpty()) {
                    throw new BusinessException("AI 测试功能需要配置 base_url。请在设置中配置后使用。");
                }
                if (setting.getApiKeyEncrypted() == null || setting.getApiKeyEncrypted().isEmpty()) {
                    throw new BusinessException("AI 测试功能需要配置 api_key。请在设置中配置后使用。");
                }
                baseUrl = setting.getApiBaseUrl();
                model = setting.getModel();
                if (model == null || model.isEmpty()) {
                    model = "gpt-4.1-mini";
                }
                try {
                    apiKey = aesUtil.decrypt(setting.getApiKeyEncrypted());
                } catch (Exception e) {
                    log.error("[DEBUG] Failed to decrypt API key: {}", e.getMessage());
                    throw new BusinessException("API Key 解密失败，请重新配置");
                }
            }
        }

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addUserMessage(request.getContent())
                .model(ChatModel.of(model))
                .build();

        SseEmitter emitter = new SseEmitter(300_000L);

        emitter.onCompletion(() -> log.debug("SSE connection completed"));
        emitter.onTimeout(() -> log.debug("SSE connection timeout"));
        emitter.onError((e) -> log.debug("SSE connection error: {}", e.getMessage()));

        Runnable task = () -> {
            try (StreamResponse<ChatCompletionChunk> streamResponse = client.chat().completions().createStreaming(params)) {
                streamResponse.stream().forEach(chunk -> {
                    chunk.choices().forEach(choice -> {
                        choice.delta().content().ifPresent(text -> {
                            try {
                                Map<String, String> payload = new HashMap<>();
                                payload.put("content", text);
                                emitter.send(SseEmitter.event().name("message").data(payload));
                            } catch (Exception e) {
                                throw new RuntimeException("SSE_CLOSED");
                            }
                        });
                    });
                });
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (RuntimeException e) {
                if ("SSE_CLOSED".equals(e.getMessage())) {
                    log.debug("SSE connection closed by client");
                } else {
                    throw e;
                }
            } catch (Exception e) {
                log.error("AI test stream failed: {}", e.getMessage());
                try {
                    Map<String, String> errorPayload = new HashMap<>();
                    errorPayload.put("error", e.getMessage());
                    emitter.send(SseEmitter.event().name("error").data(errorPayload));
                    emitter.complete();
                } catch (Exception ex) {
                    log.debug("Failed to send error event: {}", ex.getMessage());
                    emitter.completeWithError(ex);
                }
            }
        };

        sseExecutor.execute(new DelegatingSecurityContextRunnable(task));

        return emitter;
    }
}

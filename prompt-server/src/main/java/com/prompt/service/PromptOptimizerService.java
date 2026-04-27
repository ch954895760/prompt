package com.prompt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.prompt.dto.PromptOptimizeRequest;
import com.prompt.dto.PromptOptimizeResponse;
import com.prompt.entity.AiProvider;
import com.prompt.exception.BusinessException;
import com.prompt.util.AesUtil;
import com.prompt.util.PromptOptimizeCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptOptimizerService {

    private final AiProviderService aiProviderService;
    private final UserSettingService userSettingService;
    private final AesUtil aesUtil;
    private final PromptOptimizeCache optimizeCache;
    private final ObjectMapper objectMapper;

    @Value("${prompt.optimize.cache.ttl:30}")
    private Integer cacheTtlMinutes;

    public PromptOptimizeResponse optimize(Long userId, PromptOptimizeRequest request) {
        String cacheKey = optimizeCache.generateCacheKey(userId, request.getPromptContent());

        // 如果不是强制刷新，先检查缓存
        if (!Boolean.TRUE.equals(request.getForceRefresh())) {
            PromptOptimizeResponse cached = optimizeCache.get(cacheKey);
            if (cached != null) {
                cached.setFromCache(true);
                log.debug("[DEBUG] Returning cached optimization result for user: {}", userId);
                return cached;
            }
        } else {
            log.debug("[DEBUG] Force refresh requested, skipping cache for user: {}", userId);
        }

        AiConfig config = getAiConfig(userId, request.getProviderId());
        String analyzeResult = callAiForAnalysis(config, request.getPromptContent());
        PromptOptimizeResponse response = parseAnalyzeResult(analyzeResult);
        response.setOriginalPrompt(request.getPromptContent());
        response.setFromCache(false);

        optimizeCache.put(cacheKey, response, cacheTtlMinutes, TimeUnit.MINUTES);
        return response;
    }

    public void clearUserCache(Long userId) {
        optimizeCache.clearByUserId(userId);
    }

    private AiConfig getAiConfig(Long userId, Long providerId) {
        AiProvider provider;

        if (providerId != null) {
            provider = aiProviderService.getEntityById(providerId, userId);
            if (provider == null) {
                throw new BusinessException("AI提供商不存在");
            }
        } else {
            // 查询默认的AI提供商
            provider = aiProviderService.getDefaultByUserId(userId);
            if (provider == null) {
                throw new BusinessException("请先配置AI提供商。请在设置中添加AI提供商后使用。");
            }
        }

        String baseUrl = provider.getApiBaseUrl();
        String model = provider.getModel();
        String apiKey;

        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new BusinessException("AI提供商未配置 base_url");
        }
        if (provider.getApiKeyEncrypted() == null || provider.getApiKeyEncrypted().isEmpty()) {
            throw new BusinessException("AI提供商未配置 api_key");
        }

        try {
            apiKey = aesUtil.decrypt(provider.getApiKeyEncrypted());
        } catch (Exception e) {
            log.error("[DEBUG] Failed to decrypt API key: {}", e.getMessage());
            throw new BusinessException("API Key 解密失败，请重新配置");
        }

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return new AiConfig(baseUrl, apiKey, model);
    }

    private String callAiForAnalysis(AiConfig config, String userPrompt) {
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .baseUrl(config.baseUrl())
                .apiKey(config.apiKey())
                .build();

        String prompt = buildOptimizePrompt(userPrompt);
        String model = config.model();

        ChatCompletionCreateParams.Builder paramsBuilder = ChatCompletionCreateParams.builder()
                .model(ChatModel.of(model));

        if ("MiniMax-M2.7".equals(model)){
            paramsBuilder.addUserMessage(prompt);
        }else {
            paramsBuilder.addSystemMessage(prompt);
        }
        // 某些模型（如 o3-mini）不支持 temperature 参数
//        if (!isTemperatureUnsupportedModel(config.model())) {
//            paramsBuilder.temperature(0.7);
//        }

        try {
            ChatCompletion completion = client.chat().completions().create(paramsBuilder.build());
            return completion.choices().get(0).message().content()
                    .orElseThrow(() -> new BusinessException("AI 返回内容为空"));
        } catch (Exception e) {
            log.error("[DEBUG] AI analysis failed: {}", e.getMessage());
            throw new BusinessException("AI 分析失败: " + e.getMessage());
        }
    }

    private boolean isTemperatureUnsupportedModel(String model) {
        if (model == null) return false;
        String lowerModel = model.toLowerCase();
        // o3 系列模型不支持 temperature
        return lowerModel.contains("o3-") || lowerModel.startsWith("o3");
    }

    private String buildOptimizePrompt(String userPrompt) {
        return String.format(
            "你是一位专业的提示词工程师。请分析以下提示词并提供优化建议。\n\n" +
            "需要分析的提示词：\n%s\n\n" +
            "请按以下JSON格式返回结果（只返回JSON，不要其他内容）：\n" +
            "{\n" +
            "  \"score\": 1-10的质量评分,\n" +
            "  \"analysis\": \"整体分析评价，100字以内\",\n" +
            "  \"suggestions\": [\n" +
            "    {\n" +
            "      \"type\": \"structure|clarity|example\",\n" +
            "      \"title\": \"建议标题，20字以内\",\n" +
            "      \"description\": \"详细描述建议内容，100字以内\",\n" +
            "      \"priority\": \"high|medium|low\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"optimizedPrompt\": \"优化后的完整提示词\"\n" +
            "}\n\n" +
            "评分标准：\n" +
            "- 10分：完美的提示词，包含角色设定、上下文、输出格式、示例\n" +
            "- 7-9分：良好的提示词，有清晰的指令但缺少某些要素\n" +
            "- 4-6分：一般的提示词，指令模糊或结构混乱\n" +
            "- 1-3分：较差的提示词，难以理解或执行\n\n" +
            "建议类型说明：\n" +
            "- structure: 结构优化，如添加角色设定、输出格式等\n" +
            "- clarity: 清晰度改进，如指令更具体、去除歧义等\n" +
            "- example: 示例补充，如添加输入输出示例\n\n" +
            "优先级说明：\n" +
            "- high: 重要建议，强烈建议采纳\n" +
            "- medium: 一般建议，推荐采纳\n" +
            "- low: 可选建议，可根据需要采纳",
            escapeJson(userPrompt));
    }

    private PromptOptimizeResponse parseAnalyzeResult(String result) {
        try {
            String jsonStr = extractJsonFromResponse(result.trim());

            JsonNode root = objectMapper.readTree(jsonStr);

            Integer score = root.has("score") ? root.get("score").asInt() : 5;
            if (score < 1) score = 1;
            if (score > 10) score = 10;

            String analysis = root.has("analysis") ? root.get("analysis").asText() : "暂无分析";
            String optimizedPrompt = root.has("optimizedPrompt") ? root.get("optimizedPrompt").asText() : "";

            List<PromptOptimizeResponse.OptimizeSuggestion> suggestions = new ArrayList<>();
            if (root.has("suggestions") && root.get("suggestions").isArray()) {
                for (JsonNode node : root.get("suggestions")) {
                    PromptOptimizeResponse.OptimizeSuggestion suggestion =
                            PromptOptimizeResponse.OptimizeSuggestion.builder()
                                    .type(node.has("type") ? node.get("type").asText() : "clarity")
                                    .title(node.has("title") ? node.get("title").asText() : "优化建议")
                                    .description(node.has("description") ? node.get("description").asText() : "")
                                    .priority(node.has("priority") ? node.get("priority").asText() : "medium")
                                    .build();
                    suggestions.add(suggestion);
                }
            }

            if (suggestions.isEmpty()) {
                suggestions.add(PromptOptimizeResponse.OptimizeSuggestion.builder()
                        .type("clarity")
                        .title("保持当前内容")
                        .description("当前提示词已经比较清晰，建议保持。")
                        .priority("low")
                        .build());
            }

            return PromptOptimizeResponse.builder()
                    .score(score)
                    .analysis(analysis)
                    .suggestions(suggestions)
                    .optimizedPrompt(optimizedPrompt)
                    .build();
        } catch (Exception e) {
            log.error("[DEBUG] Failed to parse AI analyze result: {}", e.getMessage());
            log.error("[DEBUG] Raw result: {}", result);

            List<PromptOptimizeResponse.OptimizeSuggestion> fallbackSuggestions = new ArrayList<>();
            fallbackSuggestions.add(PromptOptimizeResponse.OptimizeSuggestion.builder()
                    .type("clarity")
                    .title("解析失败")
                    .description("无法解析AI返回的优化建议，请稍后重试。")
                    .priority("medium")
                    .build());
            return PromptOptimizeResponse.builder()
                    .score(5)
                    .analysis("解析AI返回结果失败，请重试")
                    .suggestions(fallbackSuggestions)
                    .optimizedPrompt("")
                    .build();
        }
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 从AI响应中提取JSON内容
     * 处理各种格式：直接JSON、Markdown代码块、带<think>标签的内容等
     */
    private String extractJsonFromResponse(String response) {
        // 1. 如果包含 <think> 标签，先提取 think 外的内容
        if (response.contains("<think>")) {
            int thinkEnd = response.lastIndexOf("</think>");
            if (thinkEnd != -1) {
                response = response.substring(thinkEnd + 8).trim();
            }
        }

        // 2. 查找 ```json 或 ``` 代码块
        int jsonStart = response.indexOf("```json");
        if (jsonStart != -1) {
            jsonStart += 7;
        } else {
            jsonStart = response.indexOf("```");
            if (jsonStart != -1) {
                jsonStart += 3;
            }
        }

        int jsonEnd = response.lastIndexOf("```");

        if (jsonStart != -1 && jsonEnd != -1 && jsonStart < jsonEnd) {
            return response.substring(jsonStart, jsonEnd).trim();
        }

        // 3. 查找 JSON 对象的起始位置 { 和结束位置 }
        int braceStart = response.indexOf("{");
        int braceEnd = response.lastIndexOf("}");

        if (braceStart != -1 && braceEnd != -1 && braceStart < braceEnd) {
            return response.substring(braceStart, braceEnd + 1).trim();
        }

        // 4. 如果都不匹配，返回原始内容（可能直接是JSON）
        return response;
    }

    private static class AiConfig {
        private final String baseUrl;
        private final String apiKey;
        private final String model;

        AiConfig(String baseUrl, String apiKey, String model) {
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.model = model;
        }

        String baseUrl() {
            return baseUrl;
        }

        String apiKey() {
            return apiKey;
        }

        String model() {
            return model;
        }
    }
}

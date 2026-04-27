package com.prompt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptOptimizeResponse {
    private Integer score;
    private String analysis;
    private List<OptimizeSuggestion> suggestions;
    private String optimizedPrompt;
    private String originalPrompt;
    private Boolean fromCache;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptimizeSuggestion {
        private String type;
        private String title;
        private String description;
        private String priority;
    }
}

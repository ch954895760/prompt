package com.prompt.dto;

import lombok.Data;

@Data
public class UserSettingUpdateRequest {
    private String theme;
    private String defaultModel;
    private String apiBaseUrl;
    private String apiKey;
    private String model;
}

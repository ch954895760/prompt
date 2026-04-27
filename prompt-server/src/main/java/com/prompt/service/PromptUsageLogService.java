package com.prompt.service;

import com.prompt.entity.Prompt;
import com.prompt.entity.PromptUsageLog;
import com.prompt.mapper.PromptUsageLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromptUsageLogService {

    private final PromptUsageLogMapper usageLogMapper;

    @Transactional
    public void recordUsage(Long userId, Long promptId, String context) {
        PromptUsageLog log = new PromptUsageLog();
        log.setUserId(userId);
        log.setPromptId(promptId);
        log.setContext(context);
        log.setCreatedAt(LocalDateTime.now());
        usageLogMapper.insert(log);
    }

    public List<Prompt> getRecentlyUsed(Long userId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        return usageLogMapper.selectRecentlyUsed(userId, limit);
    }
}

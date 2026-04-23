package com.prompt.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.prompt.dto.PromptCreateRequest;
import com.prompt.entity.Category;
import com.prompt.entity.Prompt;
import com.prompt.entity.PromptHistory;
import com.prompt.entity.Tag;
import com.prompt.exception.BusinessException;
import com.prompt.mapper.CategoryMapper;
import com.prompt.mapper.PromptHistoryMapper;
import com.prompt.mapper.PromptMapper;
import com.prompt.mapper.PromptTagMapper;
import com.prompt.mapper.TagMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptService {

    private final PromptMapper promptMapper;
    private final PromptHistoryMapper promptHistoryMapper;
    private final PromptTagMapper promptTagMapper;
    private final TagMapper tagMapper;
    private final CategoryMapper categoryMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Page<Prompt> page(Long userId, Long categoryId, Long tagId, String keyword, Page<Prompt> page) {
        LambdaQueryWrapper<Prompt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Prompt::getUserId, userId);
        if (categoryId != null) {
            wrapper.eq(Prompt::getCategoryId, categoryId);
        }
        if (tagId != null) {
            List<Long> promptIds = promptTagMapper.selectPromptIdsByTagId(tagId);
            if (promptIds.isEmpty()) {
                return new Page<>(page.getCurrent(), page.getSize(), 0);
            }
            wrapper.in(Prompt::getId, promptIds);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Prompt::getTitle, keyword)
                    .or()
                    .like(Prompt::getContent, keyword));
        }
        wrapper.orderByDesc(Prompt::getUpdatedAt);
        Page<Prompt> result = promptMapper.selectPage(page, wrapper);

        List<Long> categoryIds = result.getRecords().stream()
                .map(Prompt::getCategoryId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (!categoryIds.isEmpty()) {
            List<Category> categories = categoryMapper.selectBatchIds(categoryIds);
            Map<Long, String> categoryNameMap = categories.stream()
                    .collect(Collectors.toMap(Category::getId, Category::getName));
            Map<Long, String> categoryColorMap = categories.stream()
                    .collect(Collectors.toMap(Category::getId, Category::getColor));
            for (Prompt prompt : result.getRecords()) {
                if (prompt.getCategoryId() != null) {
                    prompt.setCategoryName(categoryNameMap.get(prompt.getCategoryId()));
                    prompt.setCategoryColor(categoryColorMap.get(prompt.getCategoryId()));
                }
            }
        }

        return result;
    }

    public List<Prompt> list(Long userId) {
        return promptMapper.selectListWithCategory(userId);
    }

    public Prompt getById(Long id, Long userId) {
        Prompt prompt = promptMapper.selectByIdWithCategory(id);
        if (prompt == null || (!prompt.getIsPublic() && !prompt.getUserId().equals(userId))) {
            throw new BusinessException("提示词不存在");
        }
        List<Long> tagIds = promptTagMapper.selectTagIdsByPromptId(id);
        if (!tagIds.isEmpty()) {
            List<Tag> tags = tagMapper.selectBatchIds(tagIds);
            prompt.setTags(tags);
        }
        return prompt;
    }

    @Transactional
    public Prompt create(PromptCreateRequest request, Long userId) {
        Prompt prompt = new Prompt();
        prompt.setUserId(userId);
        prompt.setTitle(request.getTitle());
        prompt.setContent(request.getContent());
        prompt.setDescription(request.getDescription());
        prompt.setCategoryId(request.getCategoryId());
        prompt.setVariablesJson(request.getVariablesJson());
        prompt.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : false);
        prompt.setUsageCount(0);
        promptMapper.insert(prompt);

        if (request.getTagIds() != null) {
            for (Long tagId : request.getTagIds()) {
                promptTagMapper.insert(prompt.getId(), tagId);
            }
        }

        saveHistory(prompt);
        return prompt;
    }

    @Transactional
    public Prompt update(Long id, PromptCreateRequest request, Long userId) {
        Prompt existing = promptMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BusinessException("提示词不存在或无权限");
        }

        existing.setTitle(request.getTitle());
        existing.setContent(request.getContent());
        existing.setDescription(request.getDescription());
        existing.setCategoryId(request.getCategoryId());
        existing.setVariablesJson(request.getVariablesJson());
        existing.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : existing.getIsPublic());
        promptMapper.updateById(existing);

        promptTagMapper.deleteByPromptId(id);
        if (request.getTagIds() != null) {
            for (Long tagId : request.getTagIds()) {
                promptTagMapper.insert(id, tagId);
            }
        }

        saveHistory(existing);
        return existing;
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Prompt prompt = promptMapper.selectById(id);
        if (prompt == null || !prompt.getUserId().equals(userId)) {
            throw new BusinessException("提示词不存在或无权限");
        }
        promptTagMapper.deleteByPromptId(id);
        promptHistoryMapper.delete(new LambdaQueryWrapper<PromptHistory>().eq(PromptHistory::getPromptId, id));
        promptMapper.deleteById(id);
    }

    @Transactional
    public void incrementUsage(Long id, Long userId) {
        Prompt prompt = promptMapper.selectById(id);
        if (prompt != null && prompt.getUserId().equals(userId)) {
            prompt.setUsageCount(prompt.getUsageCount() + 1);
            promptMapper.updateById(prompt);
        }
    }

    @Transactional
    public void rollbackToVersion(Long id, Integer version, Long userId) {
        Prompt prompt = promptMapper.selectById(id);
        if (prompt == null || !prompt.getUserId().equals(userId)) {
            throw new BusinessException("提示词不存在或无权限");
        }
        PromptHistory history = promptHistoryMapper.selectByPromptIdAndVersion(id, version);
        if (history == null) {
            throw new BusinessException("历史版本不存在");
        }
        prompt.setContent(history.getContent());
        promptMapper.updateById(prompt);
        saveHistory(prompt);
    }

    public String exportToJson(List<Prompt> prompts) {
        try {
            return objectMapper.writeValueAsString(prompts);
        } catch (Exception e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    @Transactional
    public int importFromJson(List<Map<String, Object>> data, Long userId) {
        int count = 0;
        for (Map<String, Object> item : data) {
            Prompt prompt = new Prompt();
            prompt.setUserId(userId);
            prompt.setTitle((String) item.get("title"));
            prompt.setContent((String) item.get("content"));
            prompt.setDescription((String) item.get("description"));
            prompt.setCategoryId(item.get("categoryId") != null ? Long.valueOf(item.get("categoryId").toString()) : null);
            prompt.setVariablesJson((String) item.get("variablesJson"));
            prompt.setIsPublic(Boolean.TRUE.equals(item.get("isPublic")));
            prompt.setUsageCount(0);
            promptMapper.insert(prompt);
            saveHistory(prompt);
            count++;
        }
        return count;
    }

    private void saveHistory(Prompt prompt) {
        Integer maxVersion = promptHistoryMapper.selectList(
                new LambdaQueryWrapper<PromptHistory>().eq(PromptHistory::getPromptId, prompt.getId())
                        .orderByDesc(PromptHistory::getVersion)
                        .last("LIMIT 1")
        ).stream().findFirst().map(PromptHistory::getVersion).orElse(0);

        PromptHistory history = new PromptHistory();
        history.setPromptId(prompt.getId());
        history.setContent(prompt.getContent());
        history.setVersion(maxVersion + 1);
        promptHistoryMapper.insert(history);
    }
}

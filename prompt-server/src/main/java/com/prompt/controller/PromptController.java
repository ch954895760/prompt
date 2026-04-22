package com.prompt.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.prompt.dto.PromptCreateRequest;
import com.prompt.entity.Prompt;
import com.prompt.service.PromptService;
import com.prompt.util.JwtUtil;
import com.prompt.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;
    private final JwtUtil jwtUtil;

    private Long getCurrentUserId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }

    @GetMapping
    public Result<Page<Prompt>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            Authentication authentication) {
        Page<Prompt> page = new Page<>(current, size);
        return Result.success(promptService.page(getCurrentUserId(authentication), categoryId, keyword, page));
    }

    @GetMapping("/list")
    public Result<List<Prompt>> list(Authentication authentication) {
        return Result.success(promptService.list(getCurrentUserId(authentication)));
    }

    @GetMapping("/{id}")
    public Result<Prompt> getById(@PathVariable Long id, Authentication authentication) {
        return Result.success(promptService.getById(id, getCurrentUserId(authentication)));
    }

    @PostMapping
    public Result<Prompt> create(@Valid @RequestBody PromptCreateRequest request, Authentication authentication) {
        return Result.success(promptService.create(request, getCurrentUserId(authentication)));
    }

    @PutMapping("/{id}")
    public Result<Prompt> update(@PathVariable Long id, @Valid @RequestBody PromptCreateRequest request, Authentication authentication) {
        return Result.success(promptService.update(id, request, getCurrentUserId(authentication)));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication authentication) {
        promptService.delete(id, getCurrentUserId(authentication));
        return Result.success();
    }

    @PostMapping("/{id}/use")
    public Result<Void> use(@PathVariable Long id, Authentication authentication) {
        promptService.incrementUsage(id, getCurrentUserId(authentication));
        return Result.success();
    }

    @GetMapping("/export/json")
    public ResponseEntity<String> exportJson(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<Prompt> prompts = promptService.list(userId);
        String json = promptService.exportToJson(prompts);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=prompts.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    @GetMapping("/export/markdown")
    public ResponseEntity<String> exportMarkdown(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<Prompt> prompts = promptService.list(userId);
        StringBuilder md = new StringBuilder("# Prompt Vault Export\n\n");
        md.append("Generated: ").append(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        md.append("---\n\n");
        for (Prompt p : prompts) {
            md.append("## ").append(p.getTitle()).append("\n\n");
            if (p.getDescription() != null && !p.getDescription().isEmpty()) {
                md.append(p.getDescription()).append("\n\n");
            }
            md.append("```\n").append(p.getContent()).append("\n```\n\n");
            if (p.getTags() != null && !p.getTags().isEmpty()) {
                md.append("**Tags:** ").append(p.getTags().stream().map(t -> "`" + t.getName() + "`").collect(Collectors.joining(", "))).append("\n\n");
            }
            md.append("---\n\n");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=prompts.md")
                .contentType(MediaType.TEXT_MARKDOWN)
                .body(md.toString());
    }

    @PostMapping("/import")
    public Result<Integer> importPrompts(@RequestBody List<Map<String, Object>> data, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        int count = promptService.importFromJson(data, userId);
        return Result.success(count);
    }
}

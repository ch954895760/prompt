package com.prompt.controller;

import com.prompt.entity.PromptHistory;
import com.prompt.mapper.PromptHistoryMapper;
import com.prompt.mapper.PromptMapper;
import com.prompt.service.PromptService;
import com.prompt.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/prompts")
@RequiredArgsConstructor
public class PromptHistoryController {

    private final PromptHistoryMapper promptHistoryMapper;
    private final PromptMapper promptMapper;
    private final PromptService promptService;

    @GetMapping("/{id}/history")
    public Result<List<PromptHistory>> getHistory(@PathVariable Long id,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        promptService.getById(id, userId);
        List<PromptHistory> list = promptHistoryMapper.selectByPromptId(id);
        return Result.success(list);
    }

    @PostMapping("/{id}/history/{version}/rollback")
    public Result<Void> rollback(@PathVariable Long id,
                                  @PathVariable Integer version,
                                  @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        promptService.rollbackToVersion(id, version, userId);
        return Result.success();
    }
}

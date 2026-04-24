package com.prompt.controller;

import com.prompt.dto.TagCreateRequest;
import com.prompt.entity.Tag;
import com.prompt.service.TagService;
import com.prompt.vo.Result;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    private Long getCurrentUserId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }

    @GetMapping
    public Result<List<Tag>> list(Authentication authentication) {
        return Result.success(tagService.list(getCurrentUserId(authentication)));
    }

    @PostMapping
    public Result<Tag> create(@Valid @RequestBody TagCreateRequest request, Authentication authentication) {
        return Result.success(tagService.create(request, getCurrentUserId(authentication)));
    }

    @PutMapping("/{id}")
    public Result<Tag> update(@PathVariable Long id, @Valid @RequestBody TagCreateRequest request, Authentication authentication) {
        return Result.success(tagService.update(id, request, getCurrentUserId(authentication)));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication authentication) {
        tagService.delete(id, getCurrentUserId(authentication));
        return Result.success();
    }
}


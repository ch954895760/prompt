package com.prompt.controller;

import com.prompt.dto.CategoryCreateRequest;
import com.prompt.entity.Category;
import com.prompt.service.CategoryService;
import com.prompt.vo.Result;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    private Long getCurrentUserId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }

    @GetMapping
    public Result<List<Category>> tree(Authentication authentication) {
        return Result.success(categoryService.tree(getCurrentUserId(authentication)));
    }

    @GetMapping("/list")
    public Result<List<Category>> list(Authentication authentication) {
        return Result.success(categoryService.list(getCurrentUserId(authentication)));
    }

    @PostMapping
    public Result<Category> create(@Valid @RequestBody CategoryCreateRequest request, Authentication authentication) {
        return Result.success(categoryService.create(request, getCurrentUserId(authentication)));
    }

    @PutMapping("/{id}")
    public Result<Category> update(@PathVariable Long id, @Valid @RequestBody CategoryCreateRequest request, Authentication authentication) {
        return Result.success(categoryService.update(id, request, getCurrentUserId(authentication)));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication authentication) {
        categoryService.delete(id, getCurrentUserId(authentication));
        return Result.success();
    }
}


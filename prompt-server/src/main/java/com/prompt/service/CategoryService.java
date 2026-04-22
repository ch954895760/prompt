package com.prompt.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.prompt.dto.CategoryCreateRequest;
import com.prompt.entity.Category;
import com.prompt.exception.BusinessException;
import com.prompt.mapper.CategoryMapper;
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
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public List<Category> tree(Long userId) {
        List<Category> all = categoryMapper.selectListWithCount(userId);
        Map<Long, List<Category>> parentMap = all.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));

        List<Category> roots = new ArrayList<>();
        for (Category cat : all) {
            if (cat.getParentId() == null) {
                cat.setChildren(parentMap.getOrDefault(cat.getId(), new ArrayList<>()));
                roots.add(cat);
            }
        }
        return roots;
    }

    public List<Category> list(Long userId) {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getUserId, userId)
                        .orderByAsc(Category::getSortOrder)
        );
    }

    public Category create(CategoryCreateRequest request, Long userId) {
        Category category = new Category();
        category.setUserId(userId);
        category.setName(request.getName());
        category.setParentId(request.getParentId());
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        category.setIcon(request.getIcon());
        category.setColor(request.getColor() != null ? request.getColor() : "#ea580c");
        categoryMapper.insert(category);
        return category;
    }

    public Category update(Long id, CategoryCreateRequest request, Long userId) {
        Category existing = categoryMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BusinessException("分类不存在或无权限");
        }
        existing.setName(request.getName());
        existing.setParentId(request.getParentId());
        existing.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : existing.getSortOrder());
        existing.setIcon(request.getIcon());
        existing.setColor(request.getColor() != null ? request.getColor() : existing.getColor());
        categoryMapper.updateById(existing);
        return existing;
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Category category = categoryMapper.selectById(id);
        if (category == null || !category.getUserId().equals(userId)) {
            throw new BusinessException("分类不存在或无权限");
        }
        List<Category> children = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().eq(Category::getParentId, id)
        );
        if (!children.isEmpty()) {
            throw new BusinessException("请先删除子分类");
        }
        categoryMapper.deleteById(id);
    }
}

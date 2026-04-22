package com.prompt.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.prompt.dto.TagCreateRequest;
import com.prompt.entity.Tag;
import com.prompt.exception.BusinessException;
import com.prompt.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagMapper tagMapper;

    public List<Tag> list(Long userId) {
        return tagMapper.selectList(
                new LambdaQueryWrapper<Tag>()
                        .eq(Tag::getUserId, userId)
                        .orderByDesc(Tag::getCreatedAt)
        );
    }

    public Tag create(TagCreateRequest request, Long userId) {
        Tag existing = tagMapper.selectOne(
                new LambdaQueryWrapper<Tag>()
                        .eq(Tag::getUserId, userId)
                        .eq(Tag::getName, request.getName())
        );
        if (existing != null) {
            throw new BusinessException("标签已存在");
        }

        Tag tag = new Tag();
        tag.setUserId(userId);
        tag.setName(request.getName());
        tag.setColor(request.getColor() != null ? request.getColor() : "#ea580c");
        tagMapper.insert(tag);
        return tag;
    }

    public Tag update(Long id, TagCreateRequest request, Long userId) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null || !tag.getUserId().equals(userId)) {
            throw new BusinessException("标签不存在或无权限");
        }
        Tag existing = tagMapper.selectOne(
                new LambdaQueryWrapper<Tag>()
                        .eq(Tag::getUserId, userId)
                        .eq(Tag::getName, request.getName())
                        .ne(Tag::getId, id)
        );
        if (existing != null) {
            throw new BusinessException("标签名称已存在");
        }
        tag.setName(request.getName());
        tag.setColor(request.getColor() != null ? request.getColor() : tag.getColor());
        tagMapper.updateById(tag);
        return tag;
    }

    public void delete(Long id, Long userId) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null || !tag.getUserId().equals(userId)) {
            throw new BusinessException("标签不存在或无权限");
        }
        tagMapper.deleteById(id);
    }
}

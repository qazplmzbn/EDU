package com.xyz.question_bank_management_system.modules.bank.service.impl;

import com.xyz.question_bank_management_system.modules.bank.dto.TagCreateRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.TagUpdateRequest;
import com.xyz.question_bank_management_system.modules.bank.entity.QbTag;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbTagMapper;
import com.xyz.question_bank_management_system.modules.bank.service.TagService;
import com.xyz.question_bank_management_system.modules.bank.vo.TagTreeNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final QbTagMapper tagMapper;

    @Override
    public List<QbTag> list(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return tagMapper.selectAll();
        }
        return tagMapper.selectByKeyword(keyword.trim());
    }

    @Override
    public List<TagTreeNode> tree() {
        List<QbTag> tags = tagMapper.selectAll();
        Map<Long, TagTreeNode> map = new HashMap<>();
        for (QbTag t : tags) {
            TagTreeNode n = new TagTreeNode();
            n.setId(t.getId());
            n.setTagName(t.getTagName());
            n.setTagCode(t.getTagCode());
            n.setParentId(t.getParentId());
            n.setTagLevel(t.getTagLevel());
            n.setTagType(t.getTagType());
            n.setSortOrder(t.getSortOrder());
            map.put(n.getId(), n);
        }

        List<TagTreeNode> roots = new ArrayList<>();
        for (TagTreeNode n : map.values()) {
            Long pid = n.getParentId();
            if (pid == null || pid == 0L || !map.containsKey(pid)) {
                roots.add(n);
            } else {
                map.get(pid).getChildren().add(n);
            }
        }

        Comparator<TagTreeNode> cmp = Comparator
                .comparing((TagTreeNode x) -> x.getSortOrder() == null ? 0 : x.getSortOrder())
                .thenComparing(TagTreeNode::getId);
        roots.sort(cmp);
        for (TagTreeNode r : roots) {
            sortRecursively(r, cmp);
        }
        return roots;
    }

    private void sortRecursively(TagTreeNode node, Comparator<TagTreeNode> cmp) {
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            return;
        }
        node.getChildren().sort(cmp);
        for (TagTreeNode c : node.getChildren()) {
            sortRecursively(c, cmp);
        }
    }

    @Override
    @Transactional
    public Long create(TagCreateRequest request) {
        if (tagMapper.countByName(request.getTagName()) > 0) {
            throw BizException.of(ErrorCode.CONFLICT, "标签名称已存在");
        }

        Long parentId = normalizeParentId(request.getParentId());
        if (parentId > 0) {
            ensureParentExists(parentId);
        }

        QbTag t = new QbTag();
        t.setTagName(request.getTagName());
        t.setTagCode(request.getTagCode());
        t.setParentId(parentId);
        t.setTagLevel(resolveTagLevel(parentId, request.getTagLevel(), 1));
        t.setTagType(request.getTagType());
        t.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        tagMapper.insert(t);
        return t.getId();
    }

    @Override
    public void update(Long id, TagUpdateRequest request) {
        QbTag t = tagMapper.selectById(id);
        if (t == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "标签不存在");
        }

        Long parentId = normalizeParentId(request.getParentId());
        if (Objects.equals(parentId, id)) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "父标签不能等于自身编号");
        }
        if (parentId > 0) {
            ensureParentExists(parentId);
        }

        t.setTagName(request.getTagName());
        t.setTagCode(request.getTagCode());
        t.setParentId(parentId);
        t.setTagLevel(resolveTagLevel(parentId, request.getTagLevel(), t.getTagLevel() == null ? 1 : t.getTagLevel()));
        t.setTagType(request.getTagType());
        t.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        tagMapper.update(t);
    }

    @Override
    public void delete(Long id) {
        QbTag t = tagMapper.selectById(id);
        if (t == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "标签不存在");
        }
        tagMapper.softDelete(id);
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    private void ensureParentExists(Long parentId) {
        QbTag parent = tagMapper.selectById(parentId);
        if (parent == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "父标签不存在");
        }
    }

    private int resolveTagLevel(Long parentId, Integer requestedTagLevel, int fallback) {
        if (requestedTagLevel != null && requestedTagLevel > 0) {
            return requestedTagLevel;
        }
        if (parentId == null || parentId == 0L) {
            return 1;
        }
        QbTag parent = tagMapper.selectById(parentId);
        if (parent == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "父标签不存在");
        }
        int parentLevel = parent.getTagLevel() == null ? fallback : parent.getTagLevel();
        return Math.max(1, parentLevel + 1);
    }
}

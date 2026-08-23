package com.xyz.question_bank_management_system.modules.bank.service;

import com.xyz.question_bank_management_system.modules.bank.dto.TagCreateRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.TagUpdateRequest;
import com.xyz.question_bank_management_system.modules.bank.entity.QbTag;
import com.xyz.question_bank_management_system.modules.bank.vo.TagTreeNode;

import java.util.List;

public interface TagService {
    List<QbTag> list(String keyword);
    List<TagTreeNode> tree();
    Long create(TagCreateRequest request);
    void update(Long id, TagUpdateRequest request);
    void delete(Long id);
}

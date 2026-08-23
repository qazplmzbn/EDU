package com.xyz.question_bank_management_system.modules.bank.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TagTreeNode {
    private Long id;
    private String tagName;
    private String tagCode;
    private Long parentId;
    private Integer tagLevel;
    private Integer tagType;
    private Integer sortOrder;
    private List<TagTreeNode> children = new ArrayList<>();
}

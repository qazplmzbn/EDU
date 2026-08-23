package com.xyz.question_bank_management_system.modules.bank.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QbTag {
    private Long id;
    private String tagName;
    private String tagCode;
    private Long parentId;
    private Integer tagLevel;
    /** 1=knowledge,2=chapter,3=custom */
    private Integer tagType;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}

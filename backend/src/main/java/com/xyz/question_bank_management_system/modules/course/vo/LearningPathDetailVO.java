package com.xyz.question_bank_management_system.modules.course.vo;

import com.xyz.question_bank_management_system.modules.learning.entity.LearningPath;
import com.xyz.question_bank_management_system.modules.learning.entity.LearningPathItem;
import lombok.Data;

import java.util.List;

@Data
public class LearningPathDetailVO {
    private LearningPath path;
    private List<LearningPathItem> items;
}

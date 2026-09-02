package com.xyz.question_bank_management_system.modules.agent.dto;

import com.xyz.question_bank_management_system.modules.agent.entity.ResourceItem;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 学生可见的资源项视图。
 *
 * <p>这是学生资源查询链路上的第三道隔离：SQL 已经过滤 {@code visibility<>'VISIBLE'}
 * 并且不 SELECT {@code grading_key_json}，本 DTO 再从字段定义上物理排除评分键、
 * 相似度指纹等内部列，确保即使上游 SQL 被改动也不会把内部字段序列化给学生。
 * 详见验收文档 CHECK-016 与第 10 节 Entity/DTO/VO 要求。
 */
@Data
public class StudentResourceItemView {
    private String itemCode;
    private String generatedQuestionCode;
    private String itemType;
    private String purpose;
    private String title;
    private String contentJson;
    private BigDecimal questionDifficulty;
    private String cognitiveLevel;
    private Integer orderNo;

    public static StudentResourceItemView of(ResourceItem item) {
        StudentResourceItemView view = new StudentResourceItemView();
        view.setItemCode(item.getItemCode());
        view.setGeneratedQuestionCode(item.getGeneratedQuestionCode());
        view.setItemType(item.getItemType());
        view.setPurpose(item.getPurpose());
        view.setTitle(item.getTitle());
        view.setContentJson(item.getContentJson());
        view.setQuestionDifficulty(item.getQuestionDifficulty());
        view.setCognitiveLevel(item.getCognitiveLevel());
        view.setOrderNo(item.getOrderNo());
        return view;
    }

    public static List<StudentResourceItemView> of(List<ResourceItem> items) {
        return items.stream().map(StudentResourceItemView::of).toList();
    }
}

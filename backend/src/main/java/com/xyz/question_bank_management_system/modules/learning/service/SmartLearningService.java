package com.xyz.question_bank_management_system.modules.learning.service;

import com.xyz.question_bank_management_system.modules.knowledge.entity.QbKnowledgePoint;
import com.xyz.question_bank_management_system.modules.learning.dto.LearningPathSnapshotSaveRequest;
import com.xyz.question_bank_management_system.modules.learning.dto.LearningResourceRecommendRequest;
import com.xyz.question_bank_management_system.modules.learning.dto.LearningResourceUpsertRequest;
import com.xyz.question_bank_management_system.modules.learning.dto.PersonalizedPracticeRequest;
import com.xyz.question_bank_management_system.modules.learning.dto.PracticeStartRequest;
import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningBehavior;
import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningResource;
import com.xyz.question_bank_management_system.modules.learning.vo.PersonalizedPracticePlanVO;
import com.xyz.question_bank_management_system.modules.learning.vo.SmartLearningVO.LearningPathRecommendation;
import com.xyz.question_bank_management_system.modules.learning.vo.SmartLearningVO.LearningPathSnapshotItem;
import com.xyz.question_bank_management_system.modules.learning.vo.SmartLearningVO.LearningPathSnapshotSaved;
import com.xyz.question_bank_management_system.modules.learning.vo.SmartLearningVO.LearningProfile;
import com.xyz.question_bank_management_system.modules.learning.vo.SmartLearningVO.LearningRecommendation;
import com.xyz.question_bank_management_system.modules.learning.vo.SmartLearningVO.ResourceRecommendationPublishResult;
import com.xyz.question_bank_management_system.modules.learning.vo.SmartLearningVO.StudentProfileReport;

import java.util.List;

public interface SmartLearningService {

    List<QbKnowledgePoint> knowledgePoints();

    Long createKnowledgePoint(QbKnowledgePoint point);

    void updateKnowledgePoint(Long id, QbKnowledgePoint point);

    void deleteKnowledgePoint(Long id);

    List<QbLearningResource> resources(String keyword, Long knowledgePointId, Integer limit);

    Long createResource(LearningResourceUpsertRequest request, Long operatorId);

    ResourceRecommendationPublishResult recommendResourceTargets(
            Long resourceId,
            LearningResourceRecommendRequest request,
            Long operatorId,
            boolean admin
    );

    void updateResource(Long id, LearningResourceUpsertRequest request);

    void deleteResource(Long id);

    Long recordBehavior(QbLearningBehavior behavior, Long userId);

    LearningProfile profile(Long userId);

    StudentProfileReport profileReport(Long userId);

    LearningRecommendation recommendations(Long userId);

    LearningPathRecommendation pathRecommendation(Long userId, String stage, String goal, Integer days);

    LearningPathSnapshotSaved savePathSnapshot(Long userId, LearningPathSnapshotSaveRequest request);

    LearningPathRecommendation pathSnapshotDetail(Long userId, Long id);

    List<LearningPathSnapshotItem> pathSnapshots(Long userId, Integer limit);

    PersonalizedPracticePlanVO personalizedPracticePlan(Long userId, PersonalizedPracticeRequest request);

    PracticeStartRequest buildPersonalizedPracticeRequest(Long userId, PersonalizedPracticeRequest request);
}

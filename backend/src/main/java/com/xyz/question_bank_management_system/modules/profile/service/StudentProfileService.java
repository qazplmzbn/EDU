package com.xyz.question_bank_management_system.modules.profile.service;

import com.xyz.question_bank_management_system.modules.profile.entity.*;
import java.time.LocalDateTime;
import java.util.List;

public interface StudentProfileService {
    void refreshAssessment(Long userId);
    void refreshAfterBehavior(Long userId, Long behaviorId, LocalDateTime occurredAt);
    int abilityScore(Long userId);
    StudentProfileSummary summary(Long userId);
    StudentProfileSnapshot createSnapshot(Long userId, String triggerType, Long triggerId);
    StudentBasicProfile basicProfile(Long userId);
    void saveBasicProfile(Long userId, StudentBasicProfile profile);
    List<StudentLearningGoal> goals(Long userId);
    Long saveGoal(Long userId, StudentLearningGoal goal);
    List<StudentLearningPreference> preferences(Long userId);
    void savePreference(Long userId, StudentLearningPreference preference);
}

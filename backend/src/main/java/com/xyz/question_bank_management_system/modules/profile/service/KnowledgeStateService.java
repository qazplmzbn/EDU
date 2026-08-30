package com.xyz.question_bank_management_system.modules.profile.service;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentKnowledgeState;
import com.xyz.question_bank_management_system.modules.profile.model.KnowledgeUpdateResult;
import com.xyz.question_bank_management_system.modules.profile.model.ValidatedInteraction;
import java.util.List;
public interface KnowledgeStateService {
    KnowledgeUpdateResult update(StudentKnowledgeState previous,List<ValidatedInteraction> interactions);
    StudentKnowledgeState recalibrate(StudentKnowledgeState initial,List<ValidatedInteraction> fullHistory);
}

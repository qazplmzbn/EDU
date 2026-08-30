package com.xyz.question_bank_management_system.modules.profile.service;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentProfileSnapshot;
import com.xyz.question_bank_management_system.modules.profile.model.ValidatedInteraction;
public interface ProfileAggregationService {StudentProfileSnapshot apply(ValidatedInteraction interaction);StudentProfileSnapshot recalibrate(Long userId,Long courseId,java.util.List<ValidatedInteraction> history);}

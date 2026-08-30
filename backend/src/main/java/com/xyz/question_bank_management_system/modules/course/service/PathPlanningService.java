package com.xyz.question_bank_management_system.modules.course.service;
import java.util.List;
public interface PathPlanningService {PlanResult plan(Long userId,Long courseId,Long targetKnowledgePointId);record PlanResult(List<Long> knowledgePointIds,String graphVersion,long profileVersion,String policyVersion) {}}

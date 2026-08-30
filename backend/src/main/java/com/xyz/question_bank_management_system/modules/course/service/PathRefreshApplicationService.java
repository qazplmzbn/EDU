package com.xyz.question_bank_management_system.modules.course.service;
import java.util.Map;
public interface PathRefreshApplicationService {Map<String,Object> create(Long userId,Long courseId,Long targetKnowledgePointId,String idempotencyKey);Map<String,Object> get(Long userId,String pathCode,boolean admin);Map<String,Object> refresh(Long userId,String pathCode,String reason,boolean admin);Map<String,Object> evaluateEvent(String pathCode,Map<String,Object> event);}

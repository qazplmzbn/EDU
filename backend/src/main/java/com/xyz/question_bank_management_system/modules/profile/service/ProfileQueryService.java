package com.xyz.question_bank_management_system.modules.profile.service;
import java.util.Collection;
import java.util.Map;
public interface ProfileQueryService {Map<String,Object> summary(Long userId,Long courseId);Map<String,Object> knowledgeStates(Long userId,Long courseId,Collection<Long> ids);Map<String,Object> resourcePreferences(Long userId,Long courseId);Map<String,Object> cognitiveProfile(Long userId,Long courseId);}

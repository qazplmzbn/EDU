package com.xyz.question_bank_management_system.modules.agent.tool;
import com.xyz.question_bank_management_system.exception.*;
import com.xyz.question_bank_management_system.modules.profile.service.ProfileQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;
@Component @RequiredArgsConstructor public class ResourcePreferenceQueryTool {private final ProfileQueryService service;public Map<String,Object> query(Long studentId,Long courseId){Map<String,Object> result=service.resourcePreferences(studentId,courseId);Object rows=result.get("preferences");if(!(rows instanceof Collection<?> c)||c.isEmpty())throw BizException.of(ErrorCode.PROFILE_EVIDENCE_MISSING,"未找到指定课程资源偏好");return result;}}

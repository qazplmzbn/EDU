package com.xyz.question_bank_management_system.modules.agent.tool;
import com.xyz.question_bank_management_system.exception.*;
import com.xyz.question_bank_management_system.modules.profile.service.ProfileQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;
@Component @RequiredArgsConstructor public class KnowledgeStateQueryTool {private final ProfileQueryService service;public Map<String,Object> query(Long studentId,Long courseId,Collection<Long> knowledgePointIds){Map<String,Object> result=service.knowledgeStates(studentId,courseId,knowledgePointIds);Object states=result.get("states");if(!(states instanceof Collection<?> c)||c.isEmpty())throw BizException.of(ErrorCode.PROFILE_EVIDENCE_MISSING,"未找到指定课程知识状态");return result;}}

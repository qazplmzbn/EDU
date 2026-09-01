package com.xyz.question_bank_management_system.modules.agent.tool;

import com.xyz.question_bank_management_system.modules.profile.service.ProfileQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KnowledgeStateQueryTool {
    private final ProfileQueryService service;

    /** Cold-start learners have no verified state yet; Blueprint applies its own conservative default. */
    public Map<String, Object> query(Long studentId, Long courseId, Collection<Long> knowledgePointIds) {
        Map<String, Object> result = service.knowledgeStates(studentId, courseId, knowledgePointIds);
        if (!(result.get("states") instanceof Collection<?>)) {
            result.put("states", List.of());
        }
        return result;
    }
}

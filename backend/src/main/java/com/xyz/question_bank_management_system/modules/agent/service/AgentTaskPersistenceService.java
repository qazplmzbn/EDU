package com.xyz.question_bank_management_system.modules.agent.service;

import com.xyz.question_bank_management_system.modules.agent.dto.TeacherAgentResourceGenerateRequest;
import com.xyz.question_bank_management_system.modules.agent.entity.AgentDecision;
import com.xyz.question_bank_management_system.modules.agent.entity.AgentReview;
import com.xyz.question_bank_management_system.modules.agent.entity.AgentStep;
import com.xyz.question_bank_management_system.modules.agent.entity.AgentTask;
import com.xyz.question_bank_management_system.modules.agent.vo.TeacherAgentResourceGenerateVO;

import java.util.List;

public interface AgentTaskPersistenceService {
    AgentTask createResourceTask(Long teacherId, Long studentId, boolean admin, TeacherAgentResourceGenerateRequest request);
    AgentTask requireReadable(String taskCode, Long teacherId, boolean admin);
    void markRunning(Long taskId);
    void attachProfileSnapshot(Long taskId, Object profile);
    void complete(AgentTask task, TeacherAgentResourceGenerateVO result);
    void fail(Long taskId, String error);
    void cancel(Long taskId);
    List<AgentStep> steps(String taskCode, Long teacherId, boolean admin);
    List<AgentReview> reviews(String taskCode, Long teacherId, boolean admin);
    List<AgentDecision> decisions(String taskCode, Long teacherId, boolean admin);
    List<AgentTask> recoverableTasks();
    void requeueInterruptedTasks();
    TeacherAgentResourceGenerateRequest requestOf(AgentTask task);
    boolean adminOf(AgentTask task);
}

package com.xyz.question_bank_management_system.modules.agent.service;

import com.xyz.question_bank_management_system.modules.agent.dto.TeacherAgentResourceGenerateRequest;
import com.xyz.question_bank_management_system.modules.agent.vo.TeacherAgentResourceGenerateVO;
import com.xyz.question_bank_management_system.modules.agent.vo.TeacherAgentResourceTaskVO;

import java.util.List;

public interface TeacherAgentResourceService {

    TeacherAgentResourceGenerateVO generate(Long teacherId, boolean admin, TeacherAgentResourceGenerateRequest request);

    TeacherAgentResourceGenerateVO.AgentDiscussionMessage discuss(Long teacherId, boolean admin, TeacherAgentResourceGenerateRequest request);

    List<TeacherAgentResourceGenerateVO.AgentDiscussionMessage> discussMeeting(Long teacherId, boolean admin, TeacherAgentResourceGenerateRequest request);

    TeacherAgentResourceTaskVO startGenerateTask(Long teacherId, boolean admin, TeacherAgentResourceGenerateRequest request);

    TeacherAgentResourceTaskVO getTaskStatus(Long teacherId, boolean admin, String taskId);

    TeacherAgentResourceTaskVO cancelTask(Long teacherId, boolean admin, String taskId);
}

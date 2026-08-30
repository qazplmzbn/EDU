package com.xyz.question_bank_management_system.modules.profile.model;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentKnowledgeState;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
@Data @AllArgsConstructor public class KnowledgeUpdateResult {private List<StudentKnowledgeState> states;private boolean changed;}

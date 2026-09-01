package com.xyz.question_bank_management_system.modules.profile.service;
import com.xyz.question_bank_management_system.modules.profile.entity.*; import org.springframework.web.multipart.MultipartFile; import java.util.List;
public interface ResumeEvidenceService { StudentResumeDocument upload(MultipartFile file,String consentVersion,Long userId); List<StudentResumeDocument> documents(Long userId); List<StudentResumeEvidence> evidence(Long resumeId,Long userId); void confirm(Long resumeId,Long evidenceId,boolean accepted,Long userId); }

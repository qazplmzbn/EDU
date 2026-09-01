package com.xyz.question_bank_management_system.modules.competency.service;
import com.xyz.question_bank_management_system.modules.competency.dto.CareerMappingReviewRequest;
import com.xyz.question_bank_management_system.modules.competency.entity.CareerMappingImportBatch;
import com.xyz.question_bank_management_system.modules.competency.entity.CareerMappingImportRow;
import com.xyz.question_bank_management_system.modules.competency.vo.CareerMappingImportVO;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
public interface CareerMappingImportService { CareerMappingImportVO importCsv(MultipartFile file,Long userId); List<CareerMappingImportBatch> batches(int limit); List<CareerMappingImportRow> rows(String batchCode,String status,int limit); java.util.Map<String,Object> summary(String batchCode); java.util.Map<String,Object> rematch(String batchCode); void review(Long rowId,CareerMappingReviewRequest request,Long userId); CareerMappingImportVO approveCandidatesAndPublish(String batchCode,Long userId); CareerMappingImportVO publish(String batchCode,Long userId); }

package com.xyz.question_bank_management_system.modules.competency.controller;
import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.competency.dto.CareerMappingReviewRequest;
import com.xyz.question_bank_management_system.modules.competency.entity.CareerMappingImportBatch;
import com.xyz.question_bank_management_system.modules.competency.entity.CareerMappingImportRow;
import com.xyz.question_bank_management_system.modules.competency.service.CareerMappingImportService;
import com.xyz.question_bank_management_system.modules.competency.vo.CareerMappingImportVO;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
@RestController @RequestMapping("/api/admin/competency/career-mappings") @PreAuthorize("hasRole('ADMIN')") @RequiredArgsConstructor public class AdminCareerMappingImportController {
 private final CareerMappingImportService service;
 @PostMapping("/imports") public ApiResponse<CareerMappingImportVO> upload(@RequestParam("file") MultipartFile file){return ApiResponse.ok(service.importCsv(file,SecurityContextUtil.getUserId()));}
 @GetMapping("/imports") public ApiResponse<List<CareerMappingImportBatch>> batches(@RequestParam(defaultValue="20") int limit){return ApiResponse.ok(service.batches(limit));}
 @GetMapping("/imports/{batchCode}/rows") public ApiResponse<List<CareerMappingImportRow>> rows(@PathVariable String batchCode,@RequestParam(required=false) String status,@RequestParam(defaultValue="100") int limit){return ApiResponse.ok(service.rows(batchCode,status,limit));}
 @GetMapping("/imports/{batchCode}/summary") public ApiResponse<java.util.Map<String,Object>> summary(@PathVariable String batchCode){return ApiResponse.ok(service.summary(batchCode));}
 @PostMapping("/imports/{batchCode}/rematch") public ApiResponse<java.util.Map<String,Object>> rematch(@PathVariable String batchCode){return ApiResponse.ok(service.rematch(batchCode));}
 @PostMapping("/imports/{batchCode}/approve-candidates-and-publish") public ApiResponse<CareerMappingImportVO> approveCandidatesAndPublish(@PathVariable String batchCode){return ApiResponse.ok(service.approveCandidatesAndPublish(batchCode,SecurityContextUtil.getUserId()));}
 @PostMapping("/rows/{rowId}/review") public ApiResponse<Void> review(@PathVariable Long rowId,@RequestBody CareerMappingReviewRequest request){service.review(rowId,request,SecurityContextUtil.getUserId());return ApiResponse.ok();}
 @PostMapping("/imports/{batchCode}/publish") public ApiResponse<CareerMappingImportVO> publish(@PathVariable String batchCode){return ApiResponse.ok(service.publish(batchCode,SecurityContextUtil.getUserId()));}
}

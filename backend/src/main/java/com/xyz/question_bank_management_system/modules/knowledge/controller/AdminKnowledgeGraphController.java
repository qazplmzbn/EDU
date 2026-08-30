package com.xyz.question_bank_management_system.modules.knowledge.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.knowledge.dto.GraphVersionRelationRequest;
import com.xyz.question_bank_management_system.modules.knowledge.service.KnowledgeGraphVersionService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminKnowledgeGraphController {
    private final KnowledgeGraphVersionService service;

    @PostMapping("/api/v1/admin/courses/{courseId}/graph-versions")
    public ApiResponse<Map<String,Object>> create(@PathVariable Long courseId,@RequestBody(required=false) Map<String,Object> body){
        return ApiResponse.ok(service.createDraft(courseId,body==null?null:String.valueOf(body.getOrDefault("description","")), SecurityContextUtil.getUserId()));
    }
    @PutMapping("/api/v1/admin/graph-versions/{versionCode}/relations")
    public ApiResponse<Map<String,Object>> relations(@PathVariable String versionCode,@RequestBody GraphVersionRelationRequest request){return ApiResponse.ok(service.replaceRelations(versionCode,request,SecurityContextUtil.getUserId()));}
    @PostMapping("/api/v1/admin/graph-versions/{versionCode}/validate") public ApiResponse<Map<String,Object>> validate(@PathVariable String versionCode){return ApiResponse.ok(service.validate(versionCode));}
    @PostMapping("/api/v1/admin/graph-versions/{versionCode}/publish") public ApiResponse<Map<String,Object>> publish(@PathVariable String versionCode){return ApiResponse.ok(service.publish(versionCode));}
}

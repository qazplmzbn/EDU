package com.xyz.question_bank_management_system.modules.course.controller;
import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.exception.*;
import com.xyz.question_bank_management_system.modules.course.entity.*;
import com.xyz.question_bank_management_system.modules.course.mapper.*;
import com.xyz.question_bank_management_system.modules.knowledge.entity.*;
import com.xyz.question_bank_management_system.modules.knowledge.mapper.*;
import com.xyz.question_bank_management_system.modules.profile.service.ProfileQueryService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/courses") @RequiredArgsConstructor @PreAuthorize("isAuthenticated()") public class V1CourseCatalogController {
 private final CourseMapper courseMapper;private final CourseChapterMapper chapterMapper;private final KnowledgePointMapper pointMapper;private final KnowledgeGraphVersionMapper graphVersionMapper;private final ProfileQueryService profileQueryService;
 @GetMapping("/{courseId}/catalog") public ApiResponse<Map<String,Object>> catalog(@PathVariable Long courseId){Course c=courseMapper.selectById(courseId);if(c==null)throw BizException.of(ErrorCode.NOT_FOUND,"课程不存在");KnowledgeGraphVersion v=graphVersionMapper.selectActive(courseId);Map<String,Object> m=new LinkedHashMap<>();m.put("course",c);m.put("chapters",chapterMapper.selectByCourse(courseId));m.put("knowledgePoints",pointMapper.selectActivePathEligibleByCourse(courseId));m.put("graphVersion",v==null?null:v.getVersionCode());return ApiResponse.ok(m);}
 @GetMapping("/{courseId}/chapters/{chapterId}") public ApiResponse<Map<String,Object>> chapter(@PathVariable Long courseId,@PathVariable Long chapterId){CourseChapter chapter=chapterMapper.selectByIdAndCourse(chapterId,courseId);if(chapter==null)throw BizException.of(ErrorCode.NOT_FOUND,"章节不存在");Map<String,Object> m=new LinkedHashMap<>();m.put("chapter",chapter);List<KnowledgePoint> points=pointMapper.selectActivePathEligibleByCourse(courseId).stream().filter(p->Objects.equals(p.getChapterId(),chapterId)).toList();m.put("knowledgePoints",points);if(SecurityContextUtil.currentRoles().contains("STUDENT"))m.put("masterySummary",profileQueryService.knowledgeStates(SecurityContextUtil.getUserId(),courseId,points.stream().map(KnowledgePoint::getId).toList()));return ApiResponse.ok(m);}
}

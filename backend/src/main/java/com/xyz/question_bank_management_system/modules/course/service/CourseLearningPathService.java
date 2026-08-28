package com.xyz.question_bank_management_system.modules.course.service;

import com.xyz.question_bank_management_system.modules.course.dto.CourseKnowledgeReplaceRequest;
import com.xyz.question_bank_management_system.modules.course.dto.CoursePathGenerateRequest;
import com.xyz.question_bank_management_system.modules.course.dto.CourseUpsertRequest;
import com.xyz.question_bank_management_system.modules.course.entity.Course;
import com.xyz.question_bank_management_system.modules.course.entity.CourseKnowledge;
import com.xyz.question_bank_management_system.modules.course.vo.CourseProgressVO;
import com.xyz.question_bank_management_system.modules.course.vo.CourseStudentProgressVO;
import com.xyz.question_bank_management_system.modules.course.vo.LearningPathDetailVO;

import java.util.List;

public interface CourseLearningPathService {
    Long createCourse(CourseUpsertRequest request, Long operatorId, boolean admin);
    void updateCourse(Long courseId, CourseUpsertRequest request, Long operatorId, boolean admin);
    void deleteCourse(Long courseId, Long operatorId, boolean admin);
    List<Course> manageableCourses(Long operatorId, boolean admin);
    List<CourseKnowledge> courseKnowledge(Long courseId, Long operatorId, boolean admin);
    void replaceCourseKnowledge(Long courseId, CourseKnowledgeReplaceRequest request, Long operatorId, boolean admin);
    List<CourseProgressVO> visibleCourses(Long studentId);
    CourseProgressVO courseProgress(Long courseId, Long studentId);
    Long generateCoursePath(Long courseId, CoursePathGenerateRequest request, Long studentId);
    List<LearningPathDetailVO> studentPaths(Long studentId);
    LearningPathDetailVO studentPath(Long pathId, Long studentId);
    LearningPathDetailVO completePathItem(Long pathId, Long itemId, Long studentId);
    List<CourseStudentProgressVO> courseStudentProgress(Long courseId, Long operatorId, boolean admin);
    LearningPathDetailVO teacherPath(Long pathId, Long operatorId, boolean admin);
}

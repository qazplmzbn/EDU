package com.xyz.question_bank_management_system.modules.course.vo;

import com.xyz.question_bank_management_system.modules.course.entity.StudentCourseProgress;
import lombok.Data;

@Data
public class CourseStudentProgressVO extends StudentCourseProgress {
    private String username;
    private String displayName;
}

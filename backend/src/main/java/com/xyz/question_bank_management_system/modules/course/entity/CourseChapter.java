package com.xyz.question_bank_management_system.modules.course.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CourseChapter {
    private Long id;
    private Long courseId;
    private String chapterCode;
    private String chapterName;
    private Integer orderNo;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.smartedu.modules.course.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StudentCourseVO {
    private Long courseId;
    private String courseName;
    private String coverImage;
    private String teacherName;
    private Integer totalLessons;
    private Integer completedLessons;
    private Integer completionRate;
    private LocalDateTime lastStudyTime;
    private String lastStudyTimeStr;
}
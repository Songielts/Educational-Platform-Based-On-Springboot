package com.smartedu.modules.study.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程学习统计VO
 */
@Data
public class CourseStatVO {

    private Long courseId;

    private String courseName;

    private String coverImage;

    private Integer totalLessons;

    private Integer completedLessons;

    private BigDecimal completionRate;

    private Integer totalStudyTime;

    private String totalStudyTimeStr;

    private LocalDateTime lastStudyTime;

    private String lastStudyTimeStr;
}

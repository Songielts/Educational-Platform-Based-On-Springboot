package com.smartedu.modules.study.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 视频学习进度VO
 */
@Data
public class VideoProgressVO {

    private Long id;

    private Long studentId;

    private Long lessonId;

    private Long courseId;

    private String lessonName;

    private Integer currentPosition;

    private Integer totalDuration;

    private BigDecimal progressPercent;

    private Integer isCompleted;

    private Integer studyCount;

    private Integer totalStudyTime;

    private LocalDateTime lastStudyTime;
}

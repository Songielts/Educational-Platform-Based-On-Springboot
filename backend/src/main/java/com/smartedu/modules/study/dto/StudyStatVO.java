package com.smartedu.modules.study.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 学习统计VO
 */
@Data
public class StudyStatVO {

    /**
     * 累计学习时长(秒)
     */
    private Integer totalStudyTime;

    private String totalStudyTimeStr;

    /**
     * 学习课程数
     */
    private Integer courseCount;

    /**
     * 已完成课程数
     */
    private Integer completedCourseCount;

    /**
     * 完成率
     */
    private BigDecimal completionRate;

    /**
     * 笔记数量
     */
    private Integer noteCount;

    /**
     * 考试次数
     */
    private Integer examCount;

    /**
     * 平均分
     */
    private BigDecimal averageScore;

    /**
     * 连续学习天数
     */
    private Integer continuousStudyDays;
}

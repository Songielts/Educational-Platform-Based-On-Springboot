package com.smartedu.modules.course.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生学习课程统计实体类
 */
@Data
@TableName("study_course_stat")
public class StudyCourseStat implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 总课时数
     */
    private Integer totalLessons;

    /**
     * 已完成课时数
     */
    private Integer completedLessons;

    /**
     * 完成率
     */
    private BigDecimal completionRate;

    /**
     * 总学习时长（秒）
     */
    private Integer totalStudyTime;

    /**
     * 最后学习时间
     */
    private LocalDateTime lastStudyTime;

    /**
     * 删除标记
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted = 0;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
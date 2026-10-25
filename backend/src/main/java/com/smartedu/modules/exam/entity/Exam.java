package com.smartedu.modules.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 试卷实体类
 */
@Data
@TableName("edu_exam")
public class Exam implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 试卷名称
     */
    private String examName;

    /**
     * 教师ID
     */
    private Long teacherId;

    /**
     * 关联课程ID
     */
    private Long courseId;

    /**
     * 关联班级ID
     */
    private Long classId;

    /**
     * 试卷类型: 1-练习 2-测验 3-考试
     */
    private Integer examType;

    /**
     * 总分
     */
    private BigDecimal totalScore;

    /**
     * 及格分
     */
    private BigDecimal passScore;

    /**
     * 时间限制(分钟)
     */
    private Integer timeLimit;

    /**
     * 题目数量
     */
    private Integer questionCount;

    /**
     * 允许重试: 0-否 1-是
     */
    private Integer allowRetry;

    /**
     * 最大重试次数
     */
    private Integer maxRetryCount;

    /**
     * 题目乱序: 0-否 1-是
     */
    private Integer shuffleQuestions;

    /**
     * 选项乱序: 0-否 1-是
     */
    private Integer shuffleOptions;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 状态: 0-草稿 10-审核中 1-已发布 2-已结束
     */
    private Integer status;

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
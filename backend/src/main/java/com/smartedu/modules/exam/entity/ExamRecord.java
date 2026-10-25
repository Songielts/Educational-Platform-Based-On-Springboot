package com.smartedu.modules.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考试记录实体类
 */
@Data
@TableName("study_exam_record")
public class ExamRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 试卷ID
     */
    private Long examId;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 提交时间
     */
    private LocalDateTime submitTime;

    /**
     * 用时(秒)
     */
    private Integer useTime;

    /**
     * 总分
     */
    private BigDecimal totalScore;

    /**
     * 获得分数
     */
    private BigDecimal obtainScore;

    /**
     * 答对题数
     */
    private Integer correctCount;

    /**
     * 答错题数
     */
    private Integer wrongCount;

    /**
     * 状态: 0-进行中 1-已提交 2-已批改
     */
    private Integer status;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 删除标记
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

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

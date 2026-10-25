package com.smartedu.modules.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生答题详情实体类
 */
@Data
@TableName("study_exam_answer")
public class ExamAnswer implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 考试记录ID
     */
    private Long recordId;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 试卷ID
     */
    private Long examId;

    /**
     * 题目ID
     */
    private Long questionId;

    /**
     * 学生答案
     */
    private String studentAnswer;

    /**
     * 是否正确: 0-错误 1-正确
     */
    private Integer isCorrect;

    /**
     * 获得分数
     */
    private BigDecimal obtainScore;

    /**
     * 答题时间
     */
    private LocalDateTime answerTime;

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
}

package com.smartedu.modules.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 试卷题目关联实体类
 */
@Data
@TableName("edu_exam_question")
public class ExamQuestion implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 试卷ID
     */
    private Long examId;

    /**
     * 题目ID
     */
    private Long questionId;

    /**
     * 题目顺序
     */
    private Integer questionOrder;

    /**
     * 本题分值
     */
    private BigDecimal score;

    /**
     * 删除标记
     */
    private Integer deleted;
}

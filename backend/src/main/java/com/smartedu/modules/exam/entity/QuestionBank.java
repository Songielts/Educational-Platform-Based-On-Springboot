package com.smartedu.modules.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 题库实体类
 */
@Data
@TableName("edu_question_bank")
public class QuestionBank implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 题库名称
     */
    private String bankName;

    /**
     * 教师ID
     */
    private Long teacherId;

    /**
     * 学科ID
     */
    private Long subjectId;

    /**
     * 题库描述
     */
    private String description;

    /**
     * 题目数量
     */
    private Integer questionCount;

    /**
     * 状态
     */
    private Integer status;

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

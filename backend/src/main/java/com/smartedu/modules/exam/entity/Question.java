package com.smartedu.modules.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目实体（修复版：correctCount 和 usageCount 改为数据库持久化字段）
 */
@Data
@TableName("exam_question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 题库ID
     */
    private Long bankId;

    /**
     * 创建者（教师ID）
     */
    private Long creatorId;

    /**
     * 教师ID（与creatorId同义，兼容旧代码）
     */
    @TableField("creator_id")
    private Long teacherId;

    /**
     * 题目类型：1-单选 2-多选 3-判断 4-填空 5-简答
     */
    private Integer questionType;

    /**
     * 题目内容
     */
    private String questionContent;

    /**
     * 题目选项（JSON格式字符串）
     */
    private String questionOptions;

    /**
     * 正确答案
     */
    private String correctAnswer;

    /**
     * 答案解析
     */
    private String answerAnalysis;

    /**
     * 难度等级：1-简单 2-中等 3-困难
     */
    private Integer difficultyLevel;

    /**
     * 知识点标签（逗号分隔）
     */
    private String knowledgeTags;

    // ===== 修复：改为数据库持久化字段（原来是 @TableField(exist = false) 导致永远是null） =====
    /**
     * 被使用次数
     */
    @TableField("usage_count")
    private Integer usageCount;

    /**
     * 被答对次数
     */
    @TableField("correct_count")
    private Integer correctCount;
    // ===== 修复结束 =====

    /**
     * 状态：0-草稿 1-已发布
     */
    private Integer status;

    /**
     * 逻辑删除 0-未删除 1-已删除
     */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
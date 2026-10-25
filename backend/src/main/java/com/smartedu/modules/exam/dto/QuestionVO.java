package com.smartedu.modules.exam.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目视图对象
 * 用于前端展示
 */
@Data
public class QuestionVO {

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
     * 题目类型：1-单选 2-多选 3-判断 4-填空 5-简答
     */
    private Integer questionType;

    /**
     * 题目类型名称
     */
    private String questionTypeName;

    /**
     * 题目内容
     */
    private String questionContent;

    /**
     * 题目选项（List<OptionVO> 或 JSON 字符串）
     */
    private Object questionOptions;

    /**
     * 正确答案
     */
    private String correctAnswer;

    /**
     * 学生答案（练习/考试后填充）
     */
    private String studentAnswer;

    /**
     * 学生答题是否正确：0-错误 1-正确
     */
    private Integer isCorrect;

    /**
     * 答案解析
     */
    private String answerAnalysis;

    /**
     * 难度等级：1-简单 2-中等 3-困难
     */
    private Integer difficultyLevel;

    /**
     * 难度等级名称
     */
    private String difficultyLevelName;

    /**
     * 知识点标签（List<String>）
     */
    private List<String> knowledgeTags;

    /**
     * 分值
     */
    private BigDecimal score;

    /**
     * 题目排序
     */
    private Integer questionOrder;

    /**
     * 来源类型：1-考试 2-练习
     */
    private Integer sourceType;

    /**
     * 来源名称
     */
    private String sourceName;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 课时名称
     */
    private String lessonName;

    /**
     * 考试名称
     */
    private String examName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 选项VO内部类
     */
    @Data
    public static class OptionVO {
        /**
         * 选项key（A、B、C、D）
         */
        private String key;

        /**
         * 选项内容
         */
        private String value;
    }
}
package com.smartedu.modules.exam.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 试卷VO
 */
@Data
public class ExamVO {

    private Long id;

    private String examName;

    private Long teacherId;

    private String teacherName;

    private Long courseId;

    private String courseName;

    private Long classId;

    private String className;

    private Integer examType;

    private String examTypeName;

    private BigDecimal totalScore;

    private BigDecimal passScore;

    private Integer timeLimit;

    private Integer questionCount;

    private Integer allowRetry;

    private Integer maxRetryCount;

    private Integer shuffleQuestions;

    private Integer shuffleOptions;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    private String statusName;

    private LocalDateTime createTime;

    /**
     * 试卷题目列表
     */
    private List<ExamQuestionVO> questions;

    @Data
    public static class ExamQuestionVO {
        private Long id;
        private Long questionId;
        private String questionContent;
        private Integer questionType;
        private String questionTypeName;
        private List<QuestionVO.OptionVO> questionOptions;
        private Integer questionOrder;
        private BigDecimal score;
    }
}

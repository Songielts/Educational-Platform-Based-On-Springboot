package com.smartedu.modules.exam.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试记录VO
 */
@Data
public class ExamRecordVO {

    private Long id;

    private Long studentId;

    private String studentName;

    private Long examId;

    private String examName;

    private LocalDateTime startTime;

    private LocalDateTime submitTime;

    private Integer useTime;

    private String useTimeStr;

    private BigDecimal totalScore;

    private BigDecimal obtainScore;

    private Integer correctCount;

    private Integer wrongCount;

    private Integer status;

    private String statusName;

    private Integer retryCount;

    private LocalDateTime createTime;

    /**
     * 答题详情
     */
    private List<ExamAnswerVO> answers;

    @Data
    public static class ExamAnswerVO {
        private Long id;
        private Long questionId;
        private String questionContent;
        private Integer questionType;
        private String studentAnswer;
        private String correctAnswer;
        private Integer isCorrect;
        private BigDecimal obtainScore;
        private BigDecimal score;
        private String answerAnalysis;
    }
}

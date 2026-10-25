package com.smartedu.modules.exam.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 试卷DTO
 */
@Data
public class ExamDTO {

    private Long id;

    @NotBlank(message = "试卷名称不能为空")
    private String examName;

    private Long courseId;

    private Long classId;

    private Integer examType;

    private BigDecimal totalScore;

    private BigDecimal passScore;

    private Integer timeLimit;

    private Integer allowRetry;

    private Integer maxRetryCount;

    private Integer shuffleQuestions;

    private Integer shuffleOptions;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /**
     * 试卷题目列表
     */
    private List<ExamQuestionDTO> questions;

    @Data
    public static class ExamQuestionDTO {
        private Long questionId;
        private Integer questionOrder;
        private BigDecimal score;
    }
}

package com.smartedu.modules.exam.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 题目DTO
 */
@Data
public class QuestionDTO {
    private Long id;

    // 改为可选
    private Long bankId;

    @NotNull(message = "题型不能为空")
    private Integer questionType;

    @NotBlank(message = "题目内容不能为空")
    private String questionContent;

    private List<OptionDTO> questionOptions;

    @NotBlank(message = "正确答案不能为空")
    private String correctAnswer;

    private String answerAnalysis;
    private Integer difficultyLevel;
    private List<String> knowledgeTags;
    private BigDecimal score;

    @Data
    public static class OptionDTO {
        private String key;
        private String value;
    }
}

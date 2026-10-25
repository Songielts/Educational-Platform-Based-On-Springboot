package com.smartedu.modules.exam.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class AIGenerateQuestionDTO {
    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @NotBlank(message = "题型不能为空")
    private String questionType;

    @NotNull(message = "题目数量不能为空")
    private Integer count;

    @NotBlank(message = "难度不能为空")
    private String difficulty;

    @NotBlank(message = "提示词不能为空")
    private String prompt;
}
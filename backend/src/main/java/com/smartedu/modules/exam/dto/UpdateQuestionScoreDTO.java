package com.smartedu.modules.exam.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateQuestionScoreDTO {

    @NotNull(message = "试卷ID不能为空")
    private Long examId;

    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    @NotNull(message = "分值不能为空")
    @Min(value = 1, message = "分值必须大于0")
    private BigDecimal score;
}
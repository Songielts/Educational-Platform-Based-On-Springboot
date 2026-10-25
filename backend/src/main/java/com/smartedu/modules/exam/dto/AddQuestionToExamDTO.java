package com.smartedu.modules.exam.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AddQuestionToExamDTO {

    @NotNull(message = "试卷ID不能为空")
    private Long examId;

    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    @Min(value = 1, message = "分值必须大于0")
    private BigDecimal score = BigDecimal.TEN;

    private Integer questionOrder;
}
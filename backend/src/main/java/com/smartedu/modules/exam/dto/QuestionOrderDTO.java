package com.smartedu.modules.exam.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class QuestionOrderDTO {
    private Long questionId;
    private Integer questionOrder;
    private BigDecimal score;
}
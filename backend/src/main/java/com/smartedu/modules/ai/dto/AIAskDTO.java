// dto/AIAskDTO.java
package com.smartedu.modules.ai.dto;

import lombok.Data;

@Data
public class AIAskDTO {
    private String question;
    private Long courseId;
}
package com.smartedu.modules.user.dto;

import lombok.Data;

@Data
public class UserQueryDTO {
    private String keyword;
    private Integer userType;
    private Long current = 1L;
    private Long size = 10L;
}

package com.smartedu.modules.user.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private Integer userType;
    private String userTypeName;
    private Integer status;
    private LocalDateTime createTime;
}

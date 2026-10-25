package com.smartedu.modules.user.dto;

import lombok.Data;

/**
 * 用户信息更新DTO
 */
@Data
public class UserUpdateDTO {

    private String realName;

    private String email;

    private String phone;

    private String avatar;

    private Integer gender;
}

package com.smartedu.modules.user.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户视图对象
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String realName;

    private String email;

    private String phone;

    private String avatar;

    private Integer gender;

    private Integer userType;

    private String userTypeName;

    private Integer status;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;

    private List<String> roles;

    private List<String> permissions;
}
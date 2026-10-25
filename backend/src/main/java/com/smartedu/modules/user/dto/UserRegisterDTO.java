package com.smartedu.modules.user.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 用户注册DTO
 */
@Data
public class UserRegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度必须在4-20个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;
    @NotBlank(message = "验证码不能为空")
    private String verifyCode;
    /**
     * 用户类型: 1-学生 2-教师
     */
    @NotBlank(message = "用户类型不能为空")
    private String userType;

    /**
     * 邮箱验证码
     */
    private String emailCode;

    /**
     * 手机号
     */
    private String phone;
}

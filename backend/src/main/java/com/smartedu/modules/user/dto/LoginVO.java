package com.smartedu.modules.user.dto;

import com.smartedu.modules.user.vo.UserVO;
import lombok.Data;

import java.util.List;

/**
 * 登录返回VO
 */
@Data
public class LoginVO {

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 令牌类型
     */
    private String tokenType;

    /**
     * 过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 用户信息 - 关键字段！
     */
    private UserVO userInfo;

    /**
     * 角色列表
     */
    private List<String> roles;

    /**
     * 权限列表
     */
    private List<String> permissions;
}
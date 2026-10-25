package com.smartedu.modules.user.controller;

import com.smartedu.common.result.Result;
import com.smartedu.modules.user.dto.*;
import com.smartedu.modules.user.entity.User;
import com.smartedu.modules.user.service.UserService;
import com.smartedu.modules.user.vo.UserVO;
import com.smartedu.security.utils.SecurityUtils;  // 修正包路径
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Validated UserLoginDTO loginDTO) {
        LoginVO loginVO = userService.login(loginDTO);
        return Result.success(loginVO);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Validated UserRegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success("注册成功");
    }

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/send-email-code")
    public Result<Void> sendEmailCode(@RequestParam String email,
                                      @RequestParam(required = false, defaultValue = "register") String type) {
        userService.sendEmailCode(email, type);
        return Result.success("验证码已发送");
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody @Validated ResetPasswordDTO resetDTO) {
        userService.resetPassword(resetDTO);
        return Result.success("密码重置成功");
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    public Result<UserVO> getUserInfo() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error("用户未登录");
        }
        UserVO userVO = userService.getUserInfo(userId);
        return Result.success(userVO);
    }
}
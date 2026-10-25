package com.smartedu.modules.system.controller;

import com.smartedu.common.result.Result;
import com.smartedu.modules.system.service.EmailService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

/**
 * 邮件验证码接口
 * 无需登录即可访问（发送验证码）
 */
@Slf4j
@RestController
@RequestMapping("/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    /**
     * 发送邮箱验证码
     * POST /email/send-code
     */
    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        log.info("发送验证码请求: email={}, scene={}", request.getEmail(), request.getScene());
        emailService.sendVerifyCode(request.getEmail(), request.getScene());
        return Result.success("验证码已发送");
    }

    /**
     * 验证邮箱验证码（供前端二次校验，非必须）
     * POST /email/verify-code
     */
    @PostMapping("/verify-code")
    public Result<Boolean> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        boolean valid = emailService.verifyCode(
                request.getEmail(),
                request.getCode(),
                request.getScene()
        );
        return Result.success(valid);
    }

    // ==================== 请求DTO ====================

    @Data
    public static class SendCodeRequest {
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;

        /** 业务场景: register/login/reset/bind */
        private String scene = "register";
    }

    @Data
    public static class VerifyCodeRequest {
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;

        @NotBlank(message = "验证码不能为空")
        private String code;

        private String scene = "register";
    }
}
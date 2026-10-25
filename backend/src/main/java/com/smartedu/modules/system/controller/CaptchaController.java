package com.smartedu.modules.system.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.smartedu.common.result.Result;
import com.smartedu.common.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 验证码控制器
 */
@RestController
@RequestMapping("/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final RedisUtils redisUtils;

    /**
     * 获取图形验证码
     */
    @GetMapping("/image")
    public Result<Map<String, String>> getCaptcha() {
        // 生成验证码
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
        String code = lineCaptcha.getCode();
        String base64Image = lineCaptcha.getImageBase64();

        // 生成唯一key
        String captchaKey = UUID.randomUUID().toString();

        // 缓存验证码，5分钟有效
        redisUtils.set("captcha:" + captchaKey, code, 5, TimeUnit.MINUTES);

        Map<String, String> result = new HashMap<>();
        result.put("captchaKey", captchaKey);
        result.put("captchaImage", "data:image/png;base64," + base64Image);

        return Result.success(result);
    }
}

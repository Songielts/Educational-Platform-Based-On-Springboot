package com.smartedu.modules.system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 邮件服务 - Redis 版本
 * 使用 Redis 存储验证码，支持分布式部署
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.email.code-expire-minutes:5}")
    private int codeExpireMinutes;

    @Value("${app.email.send-limit:10}")
    private int dailySendLimit;

    /** Redis key 前缀 */
    private static final String CODE_KEY_PREFIX = "email:code:";
    private static final String LIMIT_KEY_PREFIX = "email:limit:";

    /**
     * 发送验证码邮件
     * @param toEmail 收件人邮箱
     * @param scene 业务场景：register/login/reset/bind
     * @return 发送的验证码
     */
    public String sendVerifyCode(String toEmail, String scene) {
        String email = toEmail.toLowerCase().trim();
        String codeKey = buildCodeKey(scene, email);
        String limitKey = buildLimitKey(email);

        // 1. 检查发送频率限制（60秒内不能重复发送）
        checkSendInterval(codeKey);

        // 2. 检查每日上限
        checkDailyLimit(limitKey);

        // 3. 生成6位数字验证码
        String code = generateCode();

        // 4. 构建邮件内容
        String subject = buildSubject(scene);
        String content = buildContent(code, scene);

        // 5. 发送邮件
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);

            // 6. 存储验证码到 Redis，设置过期时间
            redisTemplate.opsForValue().set(codeKey, code, Duration.ofMinutes(codeExpireMinutes));

            // 7. 更新发送次数（24小时过期）
            Long count = redisTemplate.opsForValue().increment(limitKey);
            if (count != null && count == 1) {
                // 第一次发送，设置24小时过期
                redisTemplate.expire(limitKey, Duration.ofHours(24));
            }

            log.info("验证码邮件发送成功: to={}, scene={}, code={}", email, scene, code);
            return code;
        } catch (Exception e) {
            log.error("验证码邮件发送失败: to={}, scene={}, error={}", email, scene, e.getMessage());
            throw new RuntimeException("邮件发送失败，请稍后重试");
        }
    }

    /**
     * 验证验证码
     * @param email 邮箱
     * @param code 用户输入的验证码
     * @param scene 业务场景
     * @return true=正确
     */
    public boolean verifyCode(String email, String code, String scene) {
        if (email == null || code == null) {
            return false;
        }
        String key = buildCodeKey(scene, email.toLowerCase().trim());
        String cachedCode = redisTemplate.opsForValue().get(key);

        if (cachedCode == null) {
            log.warn("验证码不存在或已过期: email={}, scene={}", email, scene);
            return false;
        }

        boolean match = cachedCode.equalsIgnoreCase(code.trim());
        if (match) {
            // 验证成功后删除验证码，防止重复使用
            redisTemplate.delete(key);
            log.info("验证码验证成功: email={}, scene={}", email, scene);
        } else {
            log.warn("验证码不匹配: email={}, scene={}", email, scene);
        }
        return match;
    }

    /**
     * 清除指定邮箱的验证码
     */
    public void clearCode(String email, String scene) {
        redisTemplate.delete(buildCodeKey(scene, email.toLowerCase().trim()));
    }

    // ==================== 私有方法 ====================

    private void checkSendInterval(String codeKey) {
        Boolean exists = redisTemplate.hasKey(codeKey);
        if (Boolean.TRUE.equals(exists)) {
            Long ttl = redisTemplate.getExpire(codeKey);
            // 如果验证码还在有效期内（说明60秒内发送过）
            if (ttl != null && ttl > (codeExpireMinutes * 60 - 60)) {
                long waitSeconds = ttl - (codeExpireMinutes * 60 - 60);
                throw new RuntimeException("发送过于频繁，请" + waitSeconds + "秒后重试");
            }
        }
    }

    private void checkDailyLimit(String limitKey) {
        String countStr = redisTemplate.opsForValue().get(limitKey);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;
        if (count >= dailySendLimit) {
            throw new RuntimeException("今日发送验证码次数已达上限，请明天再试");
        }
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append((int) (Math.random() * 10));
        }
        return sb.toString();
    }

    private String buildCodeKey(String scene, String email) {
        return CODE_KEY_PREFIX + scene + ":" + email;
    }

    private String buildLimitKey(String email) {
        return LIMIT_KEY_PREFIX + email;
    }

    private String buildSubject(String scene) {
        String sceneName = switch (scene) {
            case "register" -> "注册账号";
            case "login" -> "登录验证";
            case "reset" -> "重置密码";
            case "bind" -> "绑定邮箱";
            default -> "安全验证";
        };
        return "【智慧教育平台】" + sceneName + "验证码";
    }

    private String buildContent(String code, String scene) {
        String sceneName = switch (scene) {
            case "register" -> "注册账号";
            case "login" -> "登录验证";
            case "reset" -> "重置密码";
            case "bind" -> "绑定邮箱";
            default -> "安全验证";
        };

        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return String.format(
                "您好！\n\n您正在进行【%s】操作，验证码为：\n\n%s\n\n该验证码 %d 分钟内有效，请勿泄露给他人。\n如非本人操作，请忽略此邮件。\n\n发送时间：%s\n智慧教育平台",
                sceneName, code, codeExpireMinutes, timeStr
        );
    }
}
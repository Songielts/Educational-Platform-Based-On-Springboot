package com.smartedu.common.result;

import lombok.Getter;

/**
 * 响应状态码枚举
 */
@Getter
public enum ResultCode {

    // 成功
    SUCCESS(200, "操作成功"),
    
    // 通用错误
    ERROR(500, "操作失败"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "拒绝访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    
    // 认证相关
    TOKEN_EXPIRED(1001, "Token已过期"),
    TOKEN_INVALID(1002, "Token无效"),
    TOKEN_MISSING(1003, "Token缺失"),
    LOGIN_ERROR(1004, "登录失败，用户名或密码错误"),
    ACCOUNT_DISABLED(1005, "账号已被禁用"),
    ACCOUNT_LOCKED(1006, "账号已被锁定"),
    CAPTCHA_ERROR(1007, "验证码错误"),
    
    // 用户相关
    USER_NOT_EXIST(2001, "用户不存在"),
    USER_ALREADY_EXIST(2002, "用户已存在"),
    USERNAME_ALREADY_EXIST(2003, "用户名已存在"),
    EMAIL_ALREADY_EXIST(2004, "邮箱已存在"),
    PHONE_ALREADY_EXIST(2005, "手机号已存在"),
    PASSWORD_ERROR(2006, "密码错误"),
    OLD_PASSWORD_ERROR(2007, "原密码错误"),
    
    // 班级相关
    CLASS_NOT_EXIST(3001, "班级不存在"),
    CLASS_CODE_ALREADY_EXIST(3002, "班级编码已存在"),
    CLASS_NAME_ALREADY_EXIST(3003, "班级名称已存在"),
    CLASS_FULL(3004, "班级已满员"),
    STUDENT_ALREADY_IN_CLASS(3005, "学生已在班级中"),
    STUDENT_NOT_IN_CLASS(3006, "学生不在班级中"),
    INVALID_JOIN_CODE(3007, "无效的班级加入码"),
    JOIN_CODE_EXPIRED(3008, "班级加入码已过期"),

    
    // 课程相关
    COURSE_NOT_EXIST(4001, "课程不存在"),
    COURSE_ALREADY_EXIST(4002, "课程已存在"),
    CHAPTER_NOT_EXIST(4003, "章节不存在"),
    LESSON_NOT_EXIST(4004, "课时不存在"),
    COURSE_NOT_PUBLISHED(4005, "课程未发布"),
    
    // 学习相关
    STUDY_PROGRESS_NOT_EXIST(5001, "学习进度不存在"),
    NOTE_NOT_EXIST(5002, "笔记不存在"),
    
    // 考试相关
    EXAM_NOT_EXIST(6001, "试卷不存在"),
    EXAM_NOT_STARTED(6002, "考试未开始"),
    EXAM_ALREADY_ENDED(6003, "考试已结束"),
    EXAM_ALREADY_SUBMITTED(6004, "考试已提交"),
    EXAM_TIME_UP(6005, "考试时间已到"),
    QUESTION_NOT_EXIST(6006, "题目不存在"),
    EXAM_RETRY_EXHAUSTED(6007, "重试次数已用完"),
    
    // 文件相关
    FILE_UPLOAD_ERROR(7001, "文件上传失败"),
    FILE_DOWNLOAD_ERROR(7002, "文件下载失败"),
    FILE_NOT_EXIST(7003, "文件不存在"),
    FILE_TOO_LARGE(7004, "文件过大"),
    FILE_TYPE_NOT_ALLOWED(7005, "文件类型不允许"),
    
    // 系统相关
    SYSTEM_ERROR(9001, "系统错误"),
    DATABASE_ERROR(9002, "数据库错误"),
    REDIS_ERROR(9003, "Redis错误"),
    MAIL_SEND_ERROR(9004, "邮件发送失败");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}

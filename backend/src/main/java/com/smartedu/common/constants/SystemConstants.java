package com.smartedu.common.constants;

/**
 * 系统常量
 */
public class SystemConstants {

    /**
     * 用户类型
     */
    public static class UserType {
        public static final Integer STUDENT = 1;
        public static final Integer TEACHER = 2;
        public static final Integer ADMIN = 3;
    }

    /**
     * 用户状态
     */
    public static class UserStatus {
        public static final Integer DISABLED = 0;
        public static final Integer ENABLED = 1;
    }

    /**
     * 角色编码
     */
    public static class RoleCode {
        public static final String ROLE_ADMIN = "ROLE_ADMIN";
        public static final String ROLE_TEACHER = "ROLE_TEACHER";
        public static final String ROLE_STUDENT = "ROLE_STUDENT";
    }

    /**
     * 课程状态
     */
    public static class CourseStatus {
        public static final Integer DRAFT = 0;
        public static final Integer PENDING = 1;
        public static final Integer PUBLISHED = 2;
        public static final Integer OFFLINE = 3;
    }

    /**
     * 班级状态
     */
    public static class ClassStatus {
        public static final Integer CLOSED = 0;
        public static final Integer OPEN = 1;
    }

    /**
     * 题目类型
     */
    public static class QuestionType {
        public static final Integer SINGLE_CHOICE = 1;
        public static final Integer MULTIPLE_CHOICE = 2;
        public static final Integer JUDGE = 3;
        public static final Integer FILL_BLANK = 4;
    }

    /**
     * 试卷状态
     */
    public static class ExamStatus {
        public static final Integer DRAFT = 0;
        public static final Integer PUBLISHED = 1;
        public static final Integer ENDED = 2;
    }

    /**
     * 考试记录状态
     */
    public static class ExamRecordStatus {
        public static final Integer IN_PROGRESS = 0;
        public static final Integer SUBMITTED = 1;
        public static final Integer GRADED = 2;
    }

    /**
     * 教育阶段
     */
    public static class EducationStage {
        public static final Integer PRIMARY = 1;
        public static final Integer MIDDLE = 2;
        public static final Integer HIGH = 3;
        public static final Integer UNIVERSITY = 4;
    }

    /**
     * 难度等级
     */
    public static class DifficultyLevel {
        public static final Integer EASY = 1;
        public static final Integer MEDIUM = 2;
        public static final Integer HARD = 3;
    }

    /**
     * Redis Key前缀
     */
    public static class RedisKey {
        public static final String TOKEN_PREFIX = "token:";
        public static final String USER_PREFIX = "user:";
        public static final String VIDEO_PROGRESS_PREFIX = "video:progress:";
        public static final String CAPTCHA_PREFIX = "captcha:";
        public static final String EMAIL_CODE_PREFIX = "email:code:";
        public static final String UPLOAD_CHUNK_PREFIX = "upload:chunk:";
        public static final String STUDY_TIME_PREFIX = "study:time:";
        public static final String EXAM_CACHE_PREFIX = "exam:cache:";
    }

    /**
     * 默认分页参数
     */
    public static class Page {
        public static final Long DEFAULT_CURRENT = 1L;
        public static final Long DEFAULT_SIZE = 10L;
        public static final Long MAX_SIZE = 100L;
    }
}

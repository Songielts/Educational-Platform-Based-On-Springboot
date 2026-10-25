-- 智慧教育平台数据库初始化脚本
-- 数据库: smart_edu
-- 字符集: utf8mb4

CREATE DATABASE IF NOT EXISTS smart_edu 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;

USE smart_edu;

-- ============================================
-- 1. 用户与权限相关表
-- ============================================

-- 用户表
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(加密)',
    email VARCHAR(100) UNIQUE COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    real_name VARCHAR(50) COMMENT '真实姓名',
    avatar VARCHAR(255) COMMENT '头像URL',
    gender TINYINT DEFAULT 0 COMMENT '性别: 0-未知 1-男 2-女',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    user_type TINYINT NOT NULL COMMENT '用户类型: 1-学生 2-教师 3-管理员',
    last_login_time DATETIME COMMENT '最后登录时间',
    last_login_ip VARCHAR(50) COMMENT '最后登录IP',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记: 0-未删除 1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_user_type (user_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    description VARCHAR(255) COMMENT '角色描述',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 用户角色关联表
CREATE TABLE sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 权限表(菜单/按钮)
CREATE TABLE sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '权限ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父权限ID',
    permission_name VARCHAR(50) NOT NULL COMMENT '权限名称',
    permission_code VARCHAR(100) NOT NULL COMMENT '权限编码',
    permission_type TINYINT NOT NULL COMMENT '权限类型: 1-菜单 2-按钮',
    path VARCHAR(255) COMMENT '路由路径',
    component VARCHAR(255) COMMENT '组件路径',
    icon VARCHAR(50) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id),
    INDEX idx_permission_type (permission_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 角色权限关联表
CREATE TABLE sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ============================================
-- 2. 班级管理相关表
-- ============================================

-- 班级表
CREATE TABLE edu_class (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '班级ID',
    class_code VARCHAR(50) NOT NULL UNIQUE COMMENT '班级编码',
    class_name VARCHAR(100) NOT NULL COMMENT '班级名称',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    description TEXT COMMENT '班级描述',
    cover_image VARCHAR(255) COMMENT '班级封面',
    max_students INT DEFAULT 100 COMMENT '最大学生数',
    current_students INT DEFAULT 0 COMMENT '当前学生数',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-关闭 1-开放',
    join_code VARCHAR(20) UNIQUE COMMENT '班级加入码',
    join_code_expire DATETIME COMMENT '加入码过期时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_class_code (class_code),
    INDEX idx_join_code (join_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- 班级学生关联表
CREATE TABLE edu_class_student (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id BIGINT NOT NULL COMMENT '班级ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-退出 1-在班',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    UNIQUE KEY uk_class_student (class_id, student_id),
    INDEX idx_class_id (class_id),
    INDEX idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级学生关联表';

-- ============================================
-- 3. 课程管理相关表
-- ============================================

-- 学科分类表
CREATE TABLE edu_subject (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '学科ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父学科ID',
    subject_name VARCHAR(50) NOT NULL COMMENT '学科名称',
    subject_code VARCHAR(50) COMMENT '学科编码',
    education_stage TINYINT COMMENT '教育阶段: 1-小学 2-初中 3-高中 4-大学',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id),
    INDEX idx_education_stage (education_stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学科分类表';

-- 课程表
CREATE TABLE edu_course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '课程ID',
    course_name VARCHAR(200) NOT NULL COMMENT '课程名称',
    course_code VARCHAR(50) UNIQUE COMMENT '课程编码',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    subject_id BIGINT COMMENT '学科ID',
    description TEXT COMMENT '课程描述',
    cover_image VARCHAR(255) COMMENT '课程封面',
    education_stage TINYINT COMMENT '教育阶段: 1-小学 2-初中 3-高中 4-大学',
    difficulty_level TINYINT DEFAULT 1 COMMENT '难度等级: 1-简单 2-中等 3-困难',
    duration_hours INT COMMENT '课程总时长(小时)',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-草稿 1-审核中 2-已发布 3-已下架',
    is_public TINYINT DEFAULT 0 COMMENT '是否公开: 0-否 1-是',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    favorite_count INT DEFAULT 0 COMMENT '收藏数',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    publish_time DATETIME COMMENT '发布时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_subject_id (subject_id),
    INDEX idx_status (status),
    INDEX idx_education_stage (education_stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- 课程章节表
CREATE TABLE edu_chapter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '章节ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父章节ID',
    chapter_name VARCHAR(200) NOT NULL COMMENT '章节名称',
    chapter_type TINYINT DEFAULT 1 COMMENT '章节类型: 1-章 2-节 3-课时',
    sort_order INT DEFAULT 0 COMMENT '排序',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_course_id (course_id),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程章节表';

-- 课时表(视频/资料)
CREATE TABLE edu_lesson (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '课时ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    chapter_id BIGINT NOT NULL COMMENT '章节ID',
    lesson_name VARCHAR(200) NOT NULL COMMENT '课时名称',
    lesson_type TINYINT DEFAULT 1 COMMENT '课时类型: 1-视频 2-文档 3-测验',
    video_url VARCHAR(500) COMMENT '视频URL',
    video_duration INT COMMENT '视频时长(秒)',
    video_size BIGINT COMMENT '视频大小(字节)',
    doc_url VARCHAR(500) COMMENT '文档URL',
    content TEXT COMMENT '内容文本',
    sort_order INT DEFAULT 0 COMMENT '排序',
    is_free TINYINT DEFAULT 0 COMMENT '是否免费: 0-否 1-是',
    status TINYINT DEFAULT 1 COMMENT '状态',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_course_id (course_id),
    INDEX idx_chapter_id (chapter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课时表';

-- 班级课程关联表
CREATE TABLE edu_class_course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id BIGINT NOT NULL COMMENT '班级ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    assign_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
    deadline DATETIME COMMENT '截止学习时间',
    status TINYINT DEFAULT 1 COMMENT '状态',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    UNIQUE KEY uk_class_course (class_id, course_id),
    INDEX idx_class_id (class_id),
    INDEX idx_course_id (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级课程关联表';

-- ============================================
-- 4. 学习记录相关表
-- ============================================

-- 视频学习进度表
CREATE TABLE study_video_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    lesson_id BIGINT NOT NULL COMMENT '课时ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    current_position INT DEFAULT 0 COMMENT '当前播放位置(秒)',
    total_duration INT COMMENT '视频总时长(秒)',
    progress_percent DECIMAL(5,2) DEFAULT 0 COMMENT '学习进度百分比',
    is_completed TINYINT DEFAULT 0 COMMENT '是否完成: 0-否 1-是',
    study_count INT DEFAULT 0 COMMENT '学习次数',
    total_study_time INT DEFAULT 0 COMMENT '累计学习时长(秒)',
    last_study_time DATETIME COMMENT '最后学习时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_student_lesson (student_id, lesson_id),
    INDEX idx_student_id (student_id),
    INDEX idx_course_id (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频学习进度表';

-- 学习笔记表
CREATE TABLE study_note (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '笔记ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    lesson_id BIGINT COMMENT '课时ID',
    note_title VARCHAR(200) COMMENT '笔记标题',
    note_content TEXT NOT NULL COMMENT '笔记内容',
    video_position INT COMMENT '视频时间点(秒)',
    is_favorite TINYINT DEFAULT 0 COMMENT '是否收藏: 0-否 1-是',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_student_id (student_id),
    INDEX idx_course_id (course_id),
    INDEX idx_lesson_id (lesson_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习笔记表';

-- 课程学习统计表
CREATE TABLE study_course_stat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    total_lessons INT DEFAULT 0 COMMENT '总课时数',
    completed_lessons INT DEFAULT 0 COMMENT '已完成课时数',
    completion_rate DECIMAL(5,2) DEFAULT 0 COMMENT '完成率',
    total_study_time INT DEFAULT 0 COMMENT '累计学习时长(秒)',
    last_study_time DATETIME COMMENT '最后学习时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_student_course (student_id, course_id),
    INDEX idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程学习统计表';

-- ============================================
-- 5. 题库与练习相关表
-- ============================================

-- 题库表
CREATE TABLE edu_question_bank (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '题库ID',
    bank_name VARCHAR(100) NOT NULL COMMENT '题库名称',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    subject_id BIGINT COMMENT '学科ID',
    description TEXT COMMENT '题库描述',
    question_count INT DEFAULT 0 COMMENT '题目数量',
    status TINYINT DEFAULT 1 COMMENT '状态',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_subject_id (subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库表';

-- 题目表
CREATE TABLE edu_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '题目ID',
    bank_id BIGINT NOT NULL COMMENT '题库ID',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    question_type TINYINT NOT NULL COMMENT '题型: 1-单选 2-多选 3-判断 4-填空',
    question_content TEXT NOT NULL COMMENT '题目内容',
    question_options JSON COMMENT '选项(JSON格式)',
    correct_answer TEXT NOT NULL COMMENT '正确答案',
    answer_analysis TEXT COMMENT '答案解析',
    difficulty_level TINYINT DEFAULT 2 COMMENT '难度: 1-简单 2-中等 3-困难',
    knowledge_tags JSON COMMENT '知识点标签(JSON数组)',
    score DECIMAL(5,2) DEFAULT 1.00 COMMENT '分值',
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    correct_count INT DEFAULT 0 COMMENT '答对次数',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_bank_id (bank_id),
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_question_type (question_type),
    INDEX idx_difficulty (difficulty_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- 试卷表
CREATE TABLE edu_exam (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '试卷ID',
    exam_name VARCHAR(200) NOT NULL COMMENT '试卷名称',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    course_id BIGINT COMMENT '关联课程ID',
    class_id BIGINT COMMENT '关联班级ID',
    exam_type TINYINT DEFAULT 1 COMMENT '试卷类型: 1-练习 2-测验 3-考试',
    total_score DECIMAL(6,2) DEFAULT 100.00 COMMENT '总分',
    pass_score DECIMAL(6,2) DEFAULT 60.00 COMMENT '及格分',
    time_limit INT COMMENT '时间限制(分钟)',
    question_count INT DEFAULT 0 COMMENT '题目数量',
    allow_retry TINYINT DEFAULT 1 COMMENT '允许重试: 0-否 1-是',
    max_retry_count INT DEFAULT 3 COMMENT '最大重试次数',
    shuffle_questions TINYINT DEFAULT 0 COMMENT '题目乱序: 0-否 1-是',
    shuffle_options TINYINT DEFAULT 0 COMMENT '选项乱序: 0-否 1-是',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-草稿 1-已发布 2-已结束',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_course_id (course_id),
    INDEX idx_class_id (class_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷表';

-- 试卷题目关联表
CREATE TABLE edu_exam_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL COMMENT '试卷ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    question_order INT DEFAULT 0 COMMENT '题目顺序',
    score DECIMAL(5,2) DEFAULT 1.00 COMMENT '本题分值',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    UNIQUE KEY uk_exam_question (exam_id, question_id),
    INDEX idx_exam_id (exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷题目关联表';

-- 学生考试记录表
CREATE TABLE study_exam_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    exam_id BIGINT NOT NULL COMMENT '试卷ID',
    start_time DATETIME COMMENT '开始时间',
    submit_time DATETIME COMMENT '提交时间',
    use_time INT COMMENT '用时(秒)',
    total_score DECIMAL(6,2) COMMENT '总分',
    obtain_score DECIMAL(6,2) COMMENT '获得分数',
    correct_count INT DEFAULT 0 COMMENT '答对题数',
    wrong_count INT DEFAULT 0 COMMENT '答错题数',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-进行中 1-已提交 2-已批改',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_student_id (student_id),
    INDEX idx_exam_id (exam_id),
    UNIQUE KEY uk_student_exam_retry (student_id, exam_id, retry_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生考试记录表';

-- 学生答题详情表
CREATE TABLE study_exam_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    record_id BIGINT NOT NULL COMMENT '考试记录ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    exam_id BIGINT NOT NULL COMMENT '试卷ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    student_answer TEXT COMMENT '学生答案',
    is_correct TINYINT COMMENT '是否正确: 0-错误 1-正确',
    obtain_score DECIMAL(5,2) DEFAULT 0 COMMENT '获得分数',
    answer_time DATETIME COMMENT '答题时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_record_id (record_id),
    INDEX idx_student_id (student_id),
    INDEX idx_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生答题详情表';

-- 错题本表
CREATE TABLE study_wrong_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    wrong_count INT DEFAULT 1 COMMENT '错误次数',
    last_wrong_time DATETIME COMMENT '最后错误时间',
    is_mastered TINYINT DEFAULT 0 COMMENT '是否已掌握: 0-否 1-是',
    mastered_time DATETIME COMMENT '掌握时间',
    note TEXT COMMENT '错题笔记',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_student_question (student_id, question_id),
    INDEX idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本表';

-- ============================================
-- 6. 系统管理相关表
-- ============================================

-- 数据字典表
CREATE TABLE sys_dict (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '字典ID',
    dict_type VARCHAR(50) NOT NULL COMMENT '字典类型',
    dict_code VARCHAR(50) NOT NULL COMMENT '字典编码',
    dict_name VARCHAR(100) NOT NULL COMMENT '字典名称',
    dict_value VARCHAR(255) COMMENT '字典值',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态',
    remark VARCHAR(255) COMMENT '备注',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_dict_type_code (dict_type, dict_code),
    INDEX idx_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典表';

-- 系统公告表
CREATE TABLE sys_notice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '公告ID',
    title VARCHAR(200) NOT NULL COMMENT '公告标题',
    content TEXT NOT NULL COMMENT '公告内容',
    notice_type TINYINT DEFAULT 1 COMMENT '公告类型: 1-普通 2-重要 3-紧急',
    target_type TINYINT DEFAULT 0 COMMENT '目标类型: 0-全部 1-学生 2-教师',
    publish_time DATETIME COMMENT '发布时间',
    expire_time DATETIME COMMENT '过期时间',
    publisher_id BIGINT COMMENT '发布人ID',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-草稿 1-已发布 2-已撤回',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status),
    INDEX idx_notice_type (notice_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告表';

-- 系统配置表
CREATE TABLE sys_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID',
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    description VARCHAR(255) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 操作日志表
CREATE TABLE sys_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    operation VARCHAR(100) COMMENT '操作描述',
    method VARCHAR(255) COMMENT '请求方法',
    request_url VARCHAR(255) COMMENT '请求URL',
    request_params TEXT COMMENT '请求参数',
    response_data TEXT COMMENT '响应数据',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '浏览器UA',
    execute_time INT COMMENT '执行时长(ms)',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-失败 1-成功',
    error_msg TEXT COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ============================================
-- 初始化数据
-- ============================================

-- 初始化角色
INSERT INTO sys_role (role_code, role_name, description) VALUES
('ROLE_ADMIN', '系统管理员', '拥有系统所有权限'),
('ROLE_TEACHER', '教师', '可以管理班级、课程、题库'),
('ROLE_STUDENT', '学生', '可以学习课程、参与练习');

-- 初始化管理员账号 (密码: admin123)
INSERT INTO sys_user (username, password, email, real_name, user_type, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', 'admin@smartedu.com', '系统管理员', 3, 1);

-- 关联管理员角色
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1);

-- 初始化学科分类
INSERT INTO edu_subject (subject_name, subject_code, education_stage, sort_order) VALUES
('语文', 'CHINESE', 1, 1),
('数学', 'MATH', 1, 2),
('英语', 'ENGLISH', 1, 3),
('物理', 'PHYSICS', 2, 4),
('化学', 'CHEMISTRY', 2, 5),
('生物', 'BIOLOGY', 2, 6),
('历史', 'HISTORY', 2, 7),
('地理', 'GEOGRAPHY', 2, 8);

-- 初始化数据字典
INSERT INTO sys_dict (dict_type, dict_code, dict_name, dict_value, sort_order) VALUES
('education_stage', 'PRIMARY', '小学', '1', 1),
('education_stage', 'MIDDLE', '初中', '2', 2),
('education_stage', 'HIGH', '高中', '3', 3),
('education_stage', 'UNIVERSITY', '大学', '4', 4),
('question_type', 'SINGLE', '单选题', '1', 1),
('question_type', 'MULTIPLE', '多选题', '2', 2),
('question_type', 'JUDGE', '判断题', '3', 3),
('question_type', 'FILL', '填空题', '4', 4),
('difficulty_level', 'EASY', '简单', '1', 1),
('difficulty_level', 'MEDIUM', '中等', '2', 2),
('difficulty_level', 'HARD', '困难', '3', 3);

-- 初始化系统配置
INSERT INTO sys_config (config_key, config_value, description) VALUES
('site.name', '智慧教育平台', '网站名称'),
('site.logo', '/logo.png', '网站Logo'),
('site.icp', '', 'ICP备案号'),
('upload.max_video_size', '524288000', '最大视频上传大小'),
('upload.max_image_size', '10485760', '最大图片上传大小'),
('study.progress_sync_interval', '60', '学习进度同步间隔(秒)'),
('study.min_watch_percent', '80', '视频完成最小观看百分比'),
('exam.max_retry_count', '3', '最大重试次数');

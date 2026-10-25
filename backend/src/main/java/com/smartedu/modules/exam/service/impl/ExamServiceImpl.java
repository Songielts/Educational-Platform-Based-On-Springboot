package com.smartedu.modules.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartedu.common.constants.SystemConstants;
import com.smartedu.common.exception.BusinessException;
import com.smartedu.common.result.ResultCode;
import com.smartedu.modules.course.entity.Course;
import com.smartedu.modules.course.mapper.CourseMapper;
import com.smartedu.modules.exam.dto.*;
import com.smartedu.modules.exam.entity.*;
import com.smartedu.modules.exam.mapper.*;
import com.smartedu.modules.exam.service.ExamService;
import com.smartedu.modules.ai.service.KimiService;
import com.smartedu.modules.user.entity.User;
import com.smartedu.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final QuestionMapper questionMapper;
    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final KimiService kimiService;

    // ==================== 查询辅助方法（兼容 deleted IS NULL）====================

    private Exam getExamById(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            exam = examMapper.selectByIdIgnoreDeleted(examId);
        }
        // 自动修复 deleted IS NULL，保证后续 updateById 正常工作
        if (exam != null && exam.getDeleted() == null) {
            examMapper.fixNullDeleted(examId);
            exam.setDeleted(0);
        }
        return exam;
    }

    private boolean isExamDeleted(Exam exam) {
        return exam == null || (exam.getDeleted() != null && exam.getDeleted() != 0);
    }

    private boolean isQuestionDeleted(Question question) {
        return question == null || (question.getDeleted() != null && question.getDeleted() != 0);
    }

    private boolean isQuestionValid(Question question) {
        return question != null && (question.getDeleted() == null || question.getDeleted() == 0);
    }

    /**
     * 安全获取题目选项为 List<Map>
     * 兼容 JacksonTypeHandler 反序列化后的 Object 类型
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getQuestionOptionsAsList(Question question) {
        if (question == null) return null;
        Object options = question.getQuestionOptions();
        if (options == null) return null;
        if (options instanceof List) {
            return (List<Map<String, Object>>) options;
        }
        // 如果是 String（原始 JSON），尝试解析
        if (options instanceof String) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue((String) options, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception e) {
                log.warn("[getQuestionOptionsAsList] 解析 JSON 失败: {}", options, e);
                return null;
            }
        }
        return null;
    }

    // ==================== 题目管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addQuestion(Long teacherId, QuestionDTO questionDTO) {
        Question question = new Question();
        BeanUtils.copyProperties(questionDTO, question);
        // 转换 questionOptions: List<OptionDTO> -> JSON字符串
        if (questionDTO.getQuestionOptions() != null && !questionDTO.getQuestionOptions().isEmpty()) {
            try {
                List<Map<String, Object>> options = questionDTO.getQuestionOptions().stream()
                        .map(opt -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("key", opt.getKey());
                            map.put("value", opt.getValue());
                            return map;
                        }).collect(Collectors.toList());
                question.setQuestionOptions(objectMapper.writeValueAsString(options));
            } catch (Exception e) {
                log.error("[addQuestion] 选项序列化失败", e);
                throw new RuntimeException("选项格式错误");
            }
        }
        question.setTeacherId(teacherId);
        question.setUsageCount(0);
        question.setCorrectCount(0);
        if (question.getDeleted() == null) {
            question.setDeleted(0);
        }
        questionMapper.insert(question);
        return question.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuestion(Long teacherId, QuestionDTO questionDTO) {
        Question question = questionMapper.selectById(questionDTO.getId());
        if (question == null) {
            throw new BusinessException(ResultCode.QUESTION_NOT_EXIST);
        }
        if (!question.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        BeanUtils.copyProperties(questionDTO, question);
        // 转换 questionOptions: List<OptionDTO> -> JSON字符串
        if (questionDTO.getQuestionOptions() != null) {
            try {
                List<Map<String, Object>> options = questionDTO.getQuestionOptions().stream()
                        .map(opt -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("key", opt.getKey());
                            map.put("value", opt.getValue());
                            return map;
                        }).collect(Collectors.toList());
                question.setQuestionOptions(objectMapper.writeValueAsString(options));
            } catch (Exception e) {
                log.error("[updateQuestion] 选项序列化失败", e);
                throw new RuntimeException("选项格式错误");
            }
        } else {
            question.setQuestionOptions(null);
        }
        questionMapper.updateById(question);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQuestion(Long teacherId, Long questionId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(ResultCode.QUESTION_NOT_EXIST);
        }
        if (!question.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        questionMapper.deleteById(questionId);
    }

    @Override
    public QuestionVO getQuestionDetail(Long questionId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(ResultCode.QUESTION_NOT_EXIST);
        }
        return convertToQuestionVO(question);
    }

    @Override
    public Page<QuestionVO> getQuestionPage(Long teacherId, Long bankId, Integer questionType, Integer difficultyLevel, Long current, Long size) {
        Page<Question> page = new Page<>(current, size);
        Page<Question> questionPage = questionMapper.selectPage(page,
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Question>()
                        .eq(Question::getTeacherId, teacherId)
                        .eq(Question::getDeleted, 0)
                        .eq(bankId != null, Question::getBankId, bankId)
                        .eq(questionType != null, Question::getQuestionType, questionType)
                        .eq(difficultyLevel != null, Question::getDifficultyLevel, difficultyLevel)
                        .orderByDesc(Question::getCreateTime));
        List<QuestionVO> records = questionPage.getRecords().stream()
                .map(this::convertToQuestionVO).collect(Collectors.toList());
        Page<QuestionVO> resultPage = new Page<>();
        BeanUtils.copyProperties(questionPage, resultPage);
        resultPage.setRecords(records);
        return resultPage;
    }

    // ==================== 试卷管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createExam(Long teacherId, ExamDTO examDTO) {
        Exam exam = new Exam();
        BeanUtils.copyProperties(examDTO, exam);
        exam.setTeacherId(teacherId);
        exam.setStatus(SystemConstants.ExamStatus.DRAFT);
        exam.setDeleted(0);
        exam.setQuestionCount(examDTO.getQuestions() != null ? examDTO.getQuestions().size() : 0);
        examMapper.insert(exam);
        if (examDTO.getQuestions() != null) {
            saveExamQuestions(exam.getId(), examDTO.getQuestions());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateExam(Long teacherId, ExamDTO examDTO) {
        Exam exam = getExamById(examDTO.getId());
        if (isExamDeleted(exam)) {
            throw new BusinessException(ResultCode.EXAM_NOT_EXIST);
        }
        if (!exam.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        BeanUtils.copyProperties(examDTO, exam);
        exam.setQuestionCount(examDTO.getQuestions() != null ? examDTO.getQuestions().size() : 0);
        examMapper.updateById(exam);
        if (examDTO.getQuestions() != null) {
            examQuestionMapper.deleteByExamId(exam.getId());
            saveExamQuestions(exam.getId(), examDTO.getQuestions());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteExam(Long teacherId, Long examId) {
        Exam exam = getExamById(examId);
        if (isExamDeleted(exam)) {
            throw new BusinessException(ResultCode.EXAM_NOT_EXIST);
        }
        if (!exam.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        examMapper.deleteById(examId);
    }

    @Override
    public ExamVO getExamDetail(Long examId) {
        Exam exam = getExamById(examId);
        if (isExamDeleted(exam)) {
            throw new BusinessException(ResultCode.EXAM_NOT_EXIST);
        }
        return convertToExamVO(exam, true);
    }

    @Override
    public Page<ExamVO> getTeacherExamPage(Long teacherId, String keyword, Long current, Long size) {
        Page<Exam> page = new Page<>(current, size);
        Page<Exam> examPage = (Page<Exam>) examMapper.selectTeacherExamPage(page, teacherId, keyword);
        List<ExamVO> records = examPage.getRecords().stream()
                .map(e -> convertToExamVO(e, false)).collect(Collectors.toList());
        Page<ExamVO> resultPage = new Page<>();
        BeanUtils.copyProperties(examPage, resultPage);
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishExam(Long teacherId, Long examId) {
        Exam exam = getExamById(examId);
        if (isExamDeleted(exam)) {
            throw new BusinessException(ResultCode.EXAM_NOT_EXIST);
        }
        if (!exam.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (exam.getQuestionCount() == null || exam.getQuestionCount() == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "试卷没有题目，不能提交审核");
        }
        // 提交审核：状态设为 10（审核中），管理员审核通过后才变为已发布
        examMapper.updateStatusIgnoreDeleted(examId, 10);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpublishExam(Long teacherId, Long examId) {
        Exam exam = getExamById(examId);
        if (isExamDeleted(exam)) {
            throw new BusinessException(ResultCode.EXAM_NOT_EXIST);
        }
        if (!exam.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        examMapper.updateStatusIgnoreDeleted(examId, SystemConstants.ExamStatus.DRAFT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void endExam(Long teacherId, Long examId) {
        Exam exam = getExamById(examId);
        if (isExamDeleted(exam)) {
            throw new BusinessException(ResultCode.EXAM_NOT_EXIST);
        }
        if (!exam.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        examMapper.updateStatusIgnoreDeleted(examId, SystemConstants.ExamStatus.ENDED);
    }

    @Override
    public Page<ExamVO> getStudentAvailableExams(Long studentId, Long current, Long size) {
        Page<Exam> page = new Page<>(current, size);
        Page<Exam> examPage = (Page<Exam>) examMapper.selectAvailableExamsForStudent(page, studentId);
        List<ExamVO> records = examPage.getRecords().stream()
                .map(e -> convertToExamVO(e, false)).collect(Collectors.toList());
        Page<ExamVO> resultPage = new Page<>();
        BeanUtils.copyProperties(examPage, resultPage);
        resultPage.setRecords(records);
        return resultPage;
    }

    // ==================== 管理员审核接口 ====================

    @Override
    public Page<ExamVO> getPendingAuditExams(String keyword, Long current, Long size) {
        Page<Exam> page = new Page<>(current, size);
        QueryWrapper<Exam> wrapper = new QueryWrapper<>();
        wrapper.and(w -> w.eq("deleted", 0).or().isNull("deleted"));
        wrapper.eq("status", 10); // 10=审核中
        if (StringUtils.hasText(keyword)) {
            wrapper.like("exam_name", keyword);
        }
        wrapper.orderByDesc("create_time");
        Page<Exam> examPage = examMapper.selectPage(page, wrapper);

        List<ExamVO> records = examPage.getRecords().stream()
                .map(e -> convertToExamVO(e, false)).collect(Collectors.toList());
        Page<ExamVO> resultPage = new Page<>();
        BeanUtils.copyProperties(examPage, resultPage);
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditExam(Long examId, boolean approved, String reason) {
        Exam exam = getExamById(examId);
        if (isExamDeleted(exam)) {
            throw new BusinessException(ResultCode.EXAM_NOT_EXIST);
        }
        if (exam.getStatus() != 10) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "该考试不在待审核状态");
        }
        if (approved) {
            examMapper.updateStatusIgnoreDeleted(examId, SystemConstants.ExamStatus.PUBLISHED);
        } else {
            // 拒绝后退回草稿状态
            examMapper.updateStatusIgnoreDeleted(examId, SystemConstants.ExamStatus.DRAFT);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteExam(Long examId) {
        Exam exam = getExamById(examId);
        if (isExamDeleted(exam)) {
            throw new BusinessException(ResultCode.EXAM_NOT_EXIST);
        }
        examMapper.deleteById(examId);
    }

    // ==================== 试卷题目管理 ====================

    @Override
    public List<ExamVO.ExamQuestionVO> getExamQuestions(Long examId) {
        List<ExamQuestion> examQuestions = examQuestionMapper.selectByExamId(examId);
        List<ExamVO.ExamQuestionVO> result = new ArrayList<>();
        for (ExamQuestion eq : examQuestions) {
            Question question = questionMapper.selectById(eq.getQuestionId());
            if (isQuestionDeleted(question)) continue;
            ExamVO.ExamQuestionVO vo = new ExamVO.ExamQuestionVO();
            vo.setId(eq.getId());
            vo.setQuestionId(question.getId());
            vo.setQuestionContent(question.getQuestionContent());
            vo.setQuestionType(question.getQuestionType());
            vo.setQuestionOrder(eq.getQuestionOrder());
            vo.setScore(eq.getScore());
            // ===== 修复：强制根据 questionType 设置名称 =====
            Integer qType3 = question.getQuestionType();
            if (qType3 != null) {
                switch (qType3) {
                    case 1: vo.setQuestionTypeName("单选题"); break;
                    case 2: vo.setQuestionTypeName("多选题"); break;
                    case 3: vo.setQuestionTypeName("判断题"); break;
                    case 4: vo.setQuestionTypeName("填空题"); break;
                    default: vo.setQuestionTypeName("未知题型"); break;
                }
            }
            List<Map<String, Object>> options = getQuestionOptionsAsList(question);
            if (options != null) {
                vo.setQuestionOptions(options.stream()
                        .map(optMap -> {
                            QuestionVO.OptionVO optionVO = new QuestionVO.OptionVO();
                            optionVO.setKey(String.valueOf(optMap.get("key")));
                            optionVO.setValue(String.valueOf(optMap.get("value")));
                            return optionVO;
                        }).collect(Collectors.toList()));
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addQuestionToExam(Long teacherId, AddQuestionToExamDTO dto) {
        Exam exam = getExamById(dto.getExamId());
        if (isExamDeleted(exam)) {
            throw new BusinessException(ResultCode.EXAM_NOT_EXIST);
        }
        if (!exam.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        Question question = questionMapper.selectById(dto.getQuestionId());
        if (isQuestionDeleted(question)) {
            throw new BusinessException(ResultCode.QUESTION_NOT_EXIST);
        }
        ExamQuestion exist = examQuestionMapper.selectByExamAndQuestion(dto.getExamId(), dto.getQuestionId());
        if (exist != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "该题目已在试卷中");
        }
        Integer maxOrder = examQuestionMapper.selectMaxOrderByExamId(dto.getExamId());
        int nextOrder = (maxOrder != null ? maxOrder : 0) + 1;
        ExamQuestion eq = new ExamQuestion();
        eq.setExamId(dto.getExamId());
        eq.setQuestionId(dto.getQuestionId());
        eq.setScore(dto.getScore() != null ? dto.getScore() : BigDecimal.TEN);
        eq.setQuestionOrder(dto.getQuestionOrder() != null ? dto.getQuestionOrder() : nextOrder);
        eq.setDeleted(0);
        examQuestionMapper.insert(eq);
        updateExamStats(dto.getExamId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeQuestionFromExam(Long teacherId, Long examId, Long questionId) {
        Exam exam = getExamById(examId);
        if (isExamDeleted(exam)) {
            throw new BusinessException(ResultCode.EXAM_NOT_EXIST);
        }
        if (!exam.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        examQuestionMapper.deleteByExamAndQuestion(examId, questionId);
        updateExamStats(examId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuestionScore(Long teacherId, UpdateQuestionScoreDTO dto) {
        Exam exam = getExamById(dto.getExamId());
        if (isExamDeleted(exam)) {
            throw new BusinessException(ResultCode.EXAM_NOT_EXIST);
        }
        if (!exam.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        examQuestionMapper.updateScoreByExamAndQuestion(dto.getExamId(), dto.getQuestionId(), dto.getScore());
        updateExamStats(dto.getExamId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reorderQuestions(Long teacherId, Long examId, List<QuestionOrderDTO> questionOrders) {
        Exam exam = getExamById(examId);
        if (isExamDeleted(exam)) {
            throw new BusinessException(ResultCode.EXAM_NOT_EXIST);
        }
        if (!exam.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        for (QuestionOrderDTO order : questionOrders) {
            examQuestionMapper.updateOrderAndScore(examId, order.getQuestionId(), order.getQuestionOrder(), order.getScore());
        }
        updateExamStats(examId);
    }

    @Override
    public List<Map<String, Object>> generateAIQuestions(Long teacherId, AIGenerateQuestionDTO dto) {
        try {
            return kimiService.generateQuestions(
                    dto.getCourseId(),
                    dto.getQuestionType(),
                    dto.getCount(),
                    dto.getDifficulty(),
                    dto.getPrompt()
            );
        } catch (Exception e) {
            log.error("[generateAIQuestions] AI出题失败: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "AI生成题目失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAiQuestions(Long teacherId, AiSaveQuestionDTO dto) {
        if (dto.getQuestions() == null || dto.getQuestions().isEmpty()) return;
        for (QuestionDTO questionDTO : dto.getQuestions()) {
            addQuestion(teacherId, questionDTO);
        }
    }

    private void updateExamStats(Long examId) {
        List<ExamQuestion> questions = examQuestionMapper.selectByExamId(examId);
        int count = questions.size();
        BigDecimal totalScore = questions.stream()
                .map(ExamQuestion::getScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        examMapper.updateStatsIgnoreDeleted(examId, count, totalScore);
    }

    // ==================== 考试参与 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamVO startExam(Long studentId, Long examId) {
        Exam exam = getExamById(examId);
        if (isExamDeleted(exam)) {
            throw new BusinessException(ResultCode.EXAM_NOT_EXIST);
        }
        // 草稿和审核中的考试都不能开始
        if (exam.getStatus() == SystemConstants.ExamStatus.DRAFT || exam.getStatus() == 10) {
            throw new BusinessException(ResultCode.EXAM_NOT_STARTED);
        }
        if (exam.getStatus() == SystemConstants.ExamStatus.ENDED) {
            throw new BusinessException(ResultCode.EXAM_ALREADY_ENDED);
        }
        if (exam.getStartTime() != null && exam.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.EXAM_NOT_STARTED);
        }
        if (exam.getEndTime() != null && exam.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.EXAM_ALREADY_ENDED);
        }
        int nextRetry = examRecordMapper.getNextRetryCount(studentId, examId);
        if (nextRetry > exam.getMaxRetryCount()) {
            throw new BusinessException(ResultCode.EXAM_RETRY_EXHAUSTED);
        }
        ExamRecord record = new ExamRecord();
        record.setStudentId(studentId);
        record.setExamId(examId);
        record.setStartTime(LocalDateTime.now());
        record.setStatus(SystemConstants.ExamRecordStatus.IN_PROGRESS);
        record.setRetryCount(nextRetry);
        record.setDeleted(0);
        examRecordMapper.insert(record);
        return convertToExamVO(exam, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamRecordVO submitExam(Long studentId, Long examId, Map<Long, String> answers) {
        Exam exam = getExamById(examId);
        if (isExamDeleted(exam)) {
            throw new BusinessException(ResultCode.EXAM_NOT_EXIST);
        }
        ExamRecord record = examRecordMapper.selectLatestByStudentAndExam(studentId, examId);
        if (record == null || record.getStatus() != SystemConstants.ExamRecordStatus.IN_PROGRESS) {
            throw new BusinessException(ResultCode.EXAM_ALREADY_SUBMITTED);
        }
        int useTime = (int) ChronoUnit.SECONDS.between(record.getStartTime(), LocalDateTime.now());
        if (exam.getTimeLimit() != null && useTime > exam.getTimeLimit() * 60) {
            throw new BusinessException(ResultCode.EXAM_TIME_UP);
        }
        List<ExamQuestion> examQuestions = examQuestionMapper.selectByExamId(examId);
        BigDecimal totalScore = BigDecimal.ZERO;
        int correctCount = 0;
        int wrongCount = 0;
        for (ExamQuestion eq : examQuestions) {
            Question question = questionMapper.selectById(eq.getQuestionId());
            if (isQuestionDeleted(question)) continue;
            String studentAnswer = answers.getOrDefault(eq.getQuestionId(), "");
            boolean isCorrect = checkAnswer(question, studentAnswer);
            BigDecimal obtainScore = isCorrect ? eq.getScore() : BigDecimal.ZERO;
            totalScore = totalScore.add(obtainScore);
            if (isCorrect) {
                correctCount++;
                // ===== 修复NullPointerException：correctCount可能为null，双重保险 =====
                Integer currentCorrect = question.getCorrectCount();
                question.setCorrectCount(currentCorrect != null ? currentCorrect + 1 : 1);
                questionMapper.updateById(question);
            } else {
                wrongCount++;
            }
            ExamAnswer answer = new ExamAnswer();
            answer.setRecordId(record.getId());
            answer.setStudentId(studentId);
            answer.setExamId(examId);
            answer.setQuestionId(eq.getQuestionId());
            answer.setStudentAnswer(studentAnswer);
            answer.setIsCorrect(isCorrect ? 1 : 0);
            answer.setObtainScore(obtainScore);
            answer.setAnswerTime(LocalDateTime.now());
            examAnswerMapper.insert(answer);
            // ===== 修复NullPointerException：usageCount可能为null，双重保险 =====
            Integer currentUsage = question.getUsageCount();
            question.setUsageCount(currentUsage != null ? currentUsage + 1 : 1);
            questionMapper.updateById(question);
        }
        record.setSubmitTime(LocalDateTime.now());
        record.setUseTime(useTime);
        record.setTotalScore(exam.getTotalScore());
        record.setObtainScore(totalScore);
        record.setCorrectCount(correctCount);
        record.setWrongCount(wrongCount);
        record.setStatus(SystemConstants.ExamRecordStatus.GRADED);
        examRecordMapper.updateById(record);
        return convertToRecordVO(record);
    }

    @Override
    public ExamRecordVO getExamRecord(Long studentId, Long recordId) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null || !record.getStudentId().equals(studentId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "考试记录不存在");
        }
        return convertToRecordVO(record);
    }

    @Override
    public Page<ExamRecordVO> getStudentExamRecords(Long studentId, Long current, Long size) {
        Page<ExamRecord> page = new Page<>(current, size);
        Page<ExamRecord> recordPage = (Page<ExamRecord>) examRecordMapper.selectStudentRecordPage(page, studentId);
        List<ExamRecordVO> records = recordPage.getRecords().stream()
                .map(this::convertToRecordVO).collect(Collectors.toList());
        Page<ExamRecordVO> resultPage = new Page<>();
        BeanUtils.copyProperties(recordPage, resultPage);
        resultPage.setRecords(records);
        return resultPage;
    }

    // ==================== 错题本 ====================

    @Override
    public List<QuestionVO> getWrongQuestions(Long studentId) {
        List<ExamAnswer> wrongAnswers = examAnswerMapper.selectWrongAnswers(studentId);
        Set<Long> questionIds = wrongAnswers.stream()
                .map(ExamAnswer::getQuestionId).collect(Collectors.toSet());
        List<QuestionVO> result = new ArrayList<>();
        for (Long questionId : questionIds) {
            Question question = questionMapper.selectById(questionId);
            if (isQuestionValid(question)) {
                result.add(convertToQuestionVO(question));
            }
        }
        return result;
    }

    @Override
    public void removeWrongQuestion(Long studentId, Long questionId) {
        List<ExamAnswer> wrongAnswers = examAnswerMapper.selectWrongAnswers(studentId);
        for (ExamAnswer answer : wrongAnswers) {
            if (answer.getQuestionId().equals(questionId)) {
                answer.setDeleted(1);
                examAnswerMapper.updateById(answer);
            }
        }
    }

    // ==================== 私有方法 ====================

    private void saveExamQuestions(Long examId, List<ExamDTO.ExamQuestionDTO> questions) {
        for (int i = 0; i < questions.size(); i++) {
            ExamDTO.ExamQuestionDTO dto = questions.get(i);
            ExamQuestion eq = new ExamQuestion();
            eq.setExamId(examId);
            eq.setQuestionId(dto.getQuestionId());
            eq.setQuestionOrder(dto.getQuestionOrder() != null ? dto.getQuestionOrder() : i);
            eq.setScore(dto.getScore() != null ? dto.getScore() : BigDecimal.ONE);
            examQuestionMapper.insert(eq);
        }
    }

    private boolean checkAnswer(Question question, String studentAnswer) {
        if (studentAnswer == null || studentAnswer.isEmpty()) {
            return false;
        }
        String correctAnswer = question.getCorrectAnswer();
        if (correctAnswer == null || correctAnswer.isEmpty()) {
            return false;
        }
        switch (question.getQuestionType()) {
            case 1:
            case 3:
                return studentAnswer.equalsIgnoreCase(correctAnswer);
            case 2:
                // ===== 修复：兼容 "C,D" 和 "CD" 两种正确答案格式 =====
                Set<String> correctSet = parseMultiAnswer(correctAnswer);
                Set<String> studentSet = parseMultiAnswer(studentAnswer);
                log.debug("[checkAnswer] 多选题对比: correctSet={}, studentSet={}, match={}",
                        correctSet, studentSet, correctSet.equals(studentSet));
                return correctSet.equals(studentSet);
            case 4:
                return studentAnswer.trim().equalsIgnoreCase(correctAnswer.trim());
            default:
                return false;
        }
    }

    /**
     * 解析多选题答案，兼容 "C,D" 和 "CD" 两种格式
     */
    private Set<String> parseMultiAnswer(String answer) {
        if (answer == null || answer.isEmpty()) {
            return Collections.emptySet();
        }
        String trimmed = answer.trim().toUpperCase();
        // 如果包含逗号，按逗号分割
        if (trimmed.contains(",")) {
            return Arrays.stream(trimmed.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }
        // 如果不含逗号（如 "CD"），逐个字符拆分
        return trimmed.chars()
                .mapToObj(c -> String.valueOf((char) c))
                .filter(s -> s.matches("[A-Z]"))  // 只保留字母
                .collect(Collectors.toSet());
    }

    private QuestionVO convertToQuestionVO(Question question) {
        QuestionVO vo = new QuestionVO();
        BeanUtils.copyProperties(question, vo);
        // ===== 修复：强制根据 questionType 设置名称，不依赖数据库字段 =====
        Integer qType = question.getQuestionType();
        if (qType != null) {
            switch (qType) {
                case 1: vo.setQuestionTypeName("单选题"); break;
                case 2: vo.setQuestionTypeName("多选题"); break;
                case 3: vo.setQuestionTypeName("判断题"); break;
                case 4: vo.setQuestionTypeName("填空题"); break;
                default: vo.setQuestionTypeName("未知题型"); break;
            }
        }
        Integer diffLevel = question.getDifficultyLevel();
        if (diffLevel != null) {
            switch (diffLevel) {
                case 1: vo.setDifficultyLevelName("简单"); break;
                case 2: vo.setDifficultyLevelName("中等"); break;
                case 3: vo.setDifficultyLevelName("困难"); break;
            }
        }
        List<Map<String, Object>> options = getQuestionOptionsAsList(question);
        if (options != null) {
            vo.setQuestionOptions(options.stream()
                    .map(optMap -> {
                        QuestionVO.OptionVO optionVO = new QuestionVO.OptionVO();
                        optionVO.setKey(String.valueOf(optMap.get("key")));
                        optionVO.setValue(String.valueOf(optMap.get("value")));
                        return optionVO;
                    }).collect(Collectors.toList()));
        }
        return vo;
    }

    private ExamVO convertToExamVO(Exam exam, boolean includeQuestions) {
        ExamVO vo = new ExamVO();
        BeanUtils.copyProperties(exam, vo);
        User teacher = userMapper.selectById(exam.getTeacherId());
        if (teacher != null) {
            vo.setTeacherName(teacher.getRealName());
        }
        if (exam.getCourseId() != null) {
            Course course = courseMapper.selectById(exam.getCourseId());
            if (course != null) {
                vo.setCourseName(course.getCourseName());
            }
        }
        if (exam.getExamType() != null) {
            switch (exam.getExamType()) {
                case 1: vo.setExamTypeName("练习"); break;
                case 2: vo.setExamTypeName("测验"); break;
                case 3: vo.setExamTypeName("考试"); break;
            }
        }
        if (exam.getStatus() != null) {
            switch (exam.getStatus()) {
                case 0: vo.setStatusName("草稿"); break;
                case 10: vo.setStatusName("审核中"); break;
                case 1: vo.setStatusName("已发布"); break;
                case 2: vo.setStatusName("已结束"); break;
            }
        }
        if (includeQuestions) {
            List<ExamQuestion> examQuestions = examQuestionMapper.selectByExamId(exam.getId());
            List<ExamVO.ExamQuestionVO> questionVOs = new ArrayList<>();
            for (ExamQuestion eq : examQuestions) {
                Question question = questionMapper.selectById(eq.getQuestionId());
                if (isQuestionValid(question)) {
                    ExamVO.ExamQuestionVO qvo = new ExamVO.ExamQuestionVO();
                    qvo.setId(eq.getId());
                    qvo.setQuestionId(question.getId());
                    qvo.setQuestionContent(question.getQuestionContent());
                    qvo.setQuestionType(question.getQuestionType());
                    qvo.setQuestionOrder(eq.getQuestionOrder());
                    qvo.setScore(eq.getScore());
                    // ===== 修复：强制根据 questionType 设置名称，不依赖数据库字段 =====
                    Integer qType2 = question.getQuestionType();
                    if (qType2 != null) {
                        switch (qType2) {
                            case 1: qvo.setQuestionTypeName("单选题"); break;
                            case 2: qvo.setQuestionTypeName("多选题"); break;
                            case 3: qvo.setQuestionTypeName("判断题"); break;
                            case 4: qvo.setQuestionTypeName("填空题"); break;
                            default: qvo.setQuestionTypeName("未知题型"); break;
                        }
                    }
                    List<Map<String, Object>> options = getQuestionOptionsAsList(question);
                    if (options != null) {
                        qvo.setQuestionOptions(options.stream()
                                .map(optMap -> {
                                    QuestionVO.OptionVO optionVO = new QuestionVO.OptionVO();
                                    optionVO.setKey(String.valueOf(optMap.get("key")));
                                    optionVO.setValue(String.valueOf(optMap.get("value")));
                                    return optionVO;
                                }).collect(Collectors.toList()));
                    }
                    questionVOs.add(qvo);
                }
            }
            vo.setQuestions(questionVOs);
        }
        return vo;
    }

    private ExamRecordVO convertToRecordVO(ExamRecord record) {
        ExamRecordVO vo = new ExamRecordVO();
        BeanUtils.copyProperties(record, vo);
        User student = userMapper.selectById(record.getStudentId());
        if (student != null) {
            vo.setStudentName(student.getRealName());
        }
        Exam exam = getExamById(record.getExamId());
        if (exam != null) {
            vo.setExamName(exam.getExamName());
        }
        if (record.getUseTime() != null) {
            int minutes = record.getUseTime() / 60;
            int seconds = record.getUseTime() % 60;
            vo.setUseTimeStr(minutes + "分" + seconds + "秒");
        }
        if (record.getStatus() != null) {
            switch (record.getStatus()) {
                case 0: vo.setStatusName("进行中"); break;
                case 1: vo.setStatusName("已提交"); break;
                case 2: vo.setStatusName("已批改"); break;
            }
        }
        List<ExamAnswer> answers = examAnswerMapper.selectByRecordId(record.getId());
        List<ExamRecordVO.ExamAnswerVO> answerVOs = new ArrayList<>();
        for (ExamAnswer answer : answers) {
            Question question = questionMapper.selectById(answer.getQuestionId());
            if (isQuestionValid(question)) {
                ExamRecordVO.ExamAnswerVO avo = new ExamRecordVO.ExamAnswerVO();
                avo.setId(answer.getId());
                avo.setQuestionId(question.getId());
                avo.setQuestionContent(question.getQuestionContent());
                avo.setQuestionType(question.getQuestionType());
                avo.setStudentAnswer(answer.getStudentAnswer());
                avo.setCorrectAnswer(question.getCorrectAnswer());
                avo.setIsCorrect(answer.getIsCorrect());
                avo.setObtainScore(answer.getObtainScore());
                avo.setScore(answer.getObtainScore());
                avo.setAnswerAnalysis(question.getAnswerAnalysis());
                answerVOs.add(avo);
            }
        }
        vo.setAnswers(answerVOs);
        return vo;
    }
}
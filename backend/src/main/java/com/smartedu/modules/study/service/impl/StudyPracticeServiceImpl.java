package com.smartedu.modules.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartedu.modules.exam.dto.QuestionVO;
import com.smartedu.modules.exam.entity.Question;
import com.smartedu.modules.exam.mapper.QuestionMapper;
import com.smartedu.modules.study.entity.LessonQuestion;
import com.smartedu.modules.study.entity.PracticeAnswer;
import com.smartedu.modules.study.mapper.LessonQuestionMapper;
import com.smartedu.modules.study.mapper.PracticeAnswerMapper;
import com.smartedu.modules.study.service.StudyPracticeService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyPracticeServiceImpl implements StudyPracticeService {

    private final LessonQuestionMapper lessonQuestionMapper;
    private final PracticeAnswerMapper practiceAnswerMapper;
    private final QuestionMapper questionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addQuestionToLesson(Long teacherId, Long lessonId, Long questionId, Integer questionOrder) {
        LessonQuestion exist = lessonQuestionMapper.selectByLessonAndQuestion(lessonId, questionId);
        if (exist != null) {
            throw new RuntimeException("该题目已关联到此课时");
        }
        LessonQuestion lq = new LessonQuestion();
        lq.setLessonId(lessonId);
        lq.setQuestionId(questionId);
        lq.setQuestionOrder(questionOrder != null ? questionOrder : 0);
        lq.setScore(new BigDecimal("1.00"));
        lessonQuestionMapper.insert(lq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeQuestionFromLesson(Long teacherId, Long lessonId, Long questionId) {
        LambdaUpdateWrapper<LessonQuestion> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(LessonQuestion::getLessonId, lessonId)
                .eq(LessonQuestion::getQuestionId, questionId);
        lessonQuestionMapper.delete(wrapper);
    }

    @Override
    public List<QuestionVO> getLessonQuestions(Long lessonId) {
        List<LessonQuestion> lqList = lessonQuestionMapper.selectByLessonId(lessonId);
        log.info("[getLessonQuestions] lessonId={}, 关联记录数={}", lessonId, lqList.size());
        if (lqList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> questionIds = lqList.stream()
                .map(LessonQuestion::getQuestionId)
                .collect(Collectors.toList());
        log.info("[getLessonQuestions] 题目IDs={}", questionIds);

        List<Question> questions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .in(Question::getId, questionIds)
        );
        log.info("[getLessonQuestions] 查询到题目数={}", questions.size());

        // 补充选项：如果 questionOptions 为 null，单独查询
        for (Question q : questions) {
            if (q == null) continue;
            log.info("[getLessonQuestions] 题目#{} questionOptions类型={}, 值={}",
                    q.getId(),
                    q.getQuestionOptions() == null ? "null" : q.getQuestionOptions().getClass().getName(),
                    q.getQuestionOptions());
            if (q.getQuestionOptions() == null) {
                String optJson = questionMapper.selectQuestionOptionsById(q.getId());
                log.info("[getLessonQuestions] 题目#{} 单独查询question_options结果={}", q.getId(), optJson);
                if (optJson != null && !optJson.trim().isEmpty()) {
                    q.setQuestionOptions(optJson);
                }
            }
        }

        Map<Long, LessonQuestion> lqMap = lqList.stream()
                .collect(Collectors.toMap(LessonQuestion::getQuestionId, q -> q));

        List<QuestionVO> result = new ArrayList<>();
        for (Question q : questions) {
            if (q == null) continue;
            LessonQuestion lq = lqMap.get(q.getId());
            result.add(convertToVO(q, lq));
        }
        log.info("[getLessonQuestions] 最终返回={}", result.size());
        return result;
    }

    @Override
    public List<QuestionVO> getPracticeQuestions(Long studentId, Long lessonId) {
        List<QuestionVO> questions = getLessonQuestions(lessonId);
        if (studentId != null && !questions.isEmpty()) {
            List<PracticeAnswer> answers = practiceAnswerMapper.selectByStudentAndLesson(studentId, lessonId);
            Map<Long, PracticeAnswer> answerMap = answers.stream()
                    .collect(Collectors.toMap(PracticeAnswer::getQuestionId, a -> a));
            for (QuestionVO vo : questions) {
                PracticeAnswer pa = answerMap.get(vo.getId());
                if (pa != null) {
                    vo.setStudentAnswer(pa.getStudentAnswer());
                    vo.setIsCorrect(pa.getIsCorrect());
                }
            }
        }
        return questions;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitPracticeAnswers(Long studentId, Long lessonId, Map<Long, String> answers) {
        if (answers == null || answers.isEmpty()) {
            throw new RuntimeException("答案不能为空");
        }
        List<LessonQuestion> lqList = lessonQuestionMapper.selectByLessonId(lessonId);
        if (lqList.isEmpty()) {
            throw new RuntimeException("该课时暂无练习题目");
        }
        List<Long> questionIds = lqList.stream()
                .map(LessonQuestion::getQuestionId)
                .collect(Collectors.toList());

        List<Question> questions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .in(Question::getId, questionIds)
        );
        // 补充选项
        for (Question q : questions) {
            if (q != null && q.getQuestionOptions() == null) {
                String optJson = questionMapper.selectQuestionOptionsById(q.getId());
                if (optJson != null && !optJson.trim().isEmpty()) {
                    q.setQuestionOptions(optJson);
                }
            }
        }
        Map<Long, Question> questionMap = questions.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Question::getId, q -> q));
        Map<Long, LessonQuestion> lqMap = lqList.stream()
                .collect(Collectors.toMap(LessonQuestion::getQuestionId, q -> q));

        for (Map.Entry<Long, String> entry : answers.entrySet()) {
            Long questionId = entry.getKey();
            String studentAnswer = entry.getValue();
            Question question = questionMap.get(questionId);
            if (question == null) continue;

            boolean isCorrect = checkAnswer(question, studentAnswer);
            BigDecimal score = isCorrect ?
                    (lqMap.get(questionId) != null && lqMap.get(questionId).getScore() != null
                            ? lqMap.get(questionId).getScore() : new BigDecimal("1.00"))
                    : BigDecimal.ZERO;

            saveOrUpdatePracticeAnswer(studentId, lessonId, questionId, studentAnswer, isCorrect, score);
        }
    }

    @Override
    public List<QuestionVO> getAllWrongQuestions(Long studentId) {
        List<QuestionVO> result = new ArrayList<>();
        List<PracticeAnswer> practiceWrongs = practiceAnswerMapper.selectWrongAnswers(studentId);
        Set<Long> practiceQuestionIds = practiceWrongs.stream()
                .map(PracticeAnswer::getQuestionId)
                .collect(Collectors.toSet());
        if (!practiceQuestionIds.isEmpty()) {
            List<Question> questions = questionMapper.selectList(
                    new LambdaQueryWrapper<Question>()
                            .in(Question::getId, practiceQuestionIds)
            );
            for (Question q : questions) {
                if (q == null) continue;
                // 补充选项
                if (q.getQuestionOptions() == null) {
                    String optJson = questionMapper.selectQuestionOptionsById(q.getId());
                    if (optJson != null && !optJson.trim().isEmpty()) {
                        q.setQuestionOptions(optJson);
                    }
                }
                PracticeAnswer pa = practiceWrongs.stream()
                        .filter(a -> a.getQuestionId().equals(q.getId()))
                        .findFirst()
                        .orElse(null);
                QuestionVO vo = convertToVO(q, null);
                if (pa != null) {
                    vo.setStudentAnswer(pa.getStudentAnswer());
                    vo.setIsCorrect(pa.getIsCorrect());
                }
                result.add(vo);
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeWrongQuestion(Long studentId, Long questionId) {
        LambdaUpdateWrapper<PracticeAnswer> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PracticeAnswer::getStudentId, studentId)
                .eq(PracticeAnswer::getQuestionId, questionId)
                .eq(PracticeAnswer::getIsCorrect, 0);
        practiceAnswerMapper.delete(wrapper);
    }

    // ==================== 私有方法 ====================

    private boolean checkAnswer(Question question, String studentAnswer) {
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) return false;
        String correctAnswer = question.getCorrectAnswer();
        if (correctAnswer == null) return false;
        if (question.getQuestionType() != null && question.getQuestionType() == 2) {
            Set<String> correctSet = Arrays.stream(correctAnswer.split(","))
                    .map(String::trim).collect(Collectors.toSet());
            Set<String> studentSet = Arrays.stream(studentAnswer.split(","))
                    .map(String::trim).collect(Collectors.toSet());
            return correctSet.equals(studentSet);
        }
        return correctAnswer.trim().equalsIgnoreCase(studentAnswer.trim());
    }

    private void saveOrUpdatePracticeAnswer(Long studentId, Long lessonId, Long questionId,
                                            String studentAnswer, boolean isCorrect, BigDecimal score) {
        PracticeAnswer exist = practiceAnswerMapper.selectByStudentLessonQuestion(studentId, lessonId, questionId);
        if (exist != null) {
            LambdaUpdateWrapper<PracticeAnswer> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(PracticeAnswer::getId, exist.getId());
            PracticeAnswer update = new PracticeAnswer();
            update.setStudentAnswer(studentAnswer);
            update.setIsCorrect(isCorrect ? 1 : 0);
            update.setObtainScore(score);
            practiceAnswerMapper.update(update, wrapper);
        } else {
            PracticeAnswer pa = new PracticeAnswer();
            pa.setStudentId(studentId);
            pa.setLessonId(lessonId);
            pa.setQuestionId(questionId);
            pa.setStudentAnswer(studentAnswer);
            pa.setIsCorrect(isCorrect ? 1 : 0);
            pa.setObtainScore(score);
            practiceAnswerMapper.insert(pa);
        }
    }

    private QuestionVO convertToVO(Question q, LessonQuestion lq) {
        QuestionVO vo = new QuestionVO();
        vo.setId(q.getId());
        vo.setQuestionType(q.getQuestionType());
        vo.setQuestionContent(q.getQuestionContent());
        vo.setCorrectAnswer(q.getCorrectAnswer());
        vo.setAnswerAnalysis(q.getAnswerAnalysis());
        vo.setDifficultyLevel(q.getDifficultyLevel());
        vo.setScore(lq != null && lq.getScore() != null ? lq.getScore() : new BigDecimal("1.00"));
        vo.setCreateTime(q.getCreateTime());

        // 处理选项（增强版：兼容多种字段名和格式）
        List<QuestionVO.OptionVO> options = parseQuestionOptionsRobust(q.getQuestionOptions());
        vo.setQuestionOptions(options);
        log.debug("[convertToVO] 题目#{}, 解析出{}个选项", q.getId(), options != null ? options.size() : 0);

        // 处理知识点标签
        vo.setKnowledgeTags(parseKnowledgeTags(q.getKnowledgeTags()));

        // 设置题型名称
        if (q.getQuestionType() != null) {
            String[] typeNames = {"", "单选题", "多选题", "判断题", "填空题", "简答题"};
            if (q.getQuestionType() > 0 && q.getQuestionType() < typeNames.length) {
                vo.setQuestionTypeName(typeNames[q.getQuestionType()]);
            }
        }
        return vo;
    }

    /**
     * 增强版选项解析：兼容多种字段名和JSON格式
     */
    @SuppressWarnings("unchecked")
    private List<QuestionVO.OptionVO> parseQuestionOptionsRobust(Object options) {
        if (options == null) return null;

        // 1. 如果已经是 List<OptionVO>，直接返回
        if (options instanceof List && !((List<?>) options).isEmpty() && ((List<?>) options).get(0) instanceof QuestionVO.OptionVO) {
            return (List<QuestionVO.OptionVO>) options;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();

            // 2. String 类型：JSON 字符串
            if (options instanceof String) {
                String optStr = ((String) options).trim();
                if (optStr.isEmpty() || "null".equalsIgnoreCase(optStr)) return null;

                // 2.1 尝试解析为 List<Map<String, Object>>（最灵活，不依赖字段名）
                try {
                    List<Map<String, Object>> maps = mapper.readValue(optStr, new TypeReference<List<Map<String, Object>>>() {});
                    log.debug("[parseQuestionOptions] 成功解析为List<Map>, 共{}项", maps.size());
                    return convertMapsToOptionVOs(maps);
                } catch (Exception e1) {
                    log.debug("[parseQuestionOptions] List<Map>解析失败: {}", e1.getMessage());
                }

                // 2.2 尝试解析为 List<OptionVO>（标准格式）
                try {
                    List<QuestionVO.OptionVO> vos = mapper.readValue(optStr,
                            mapper.getTypeFactory().constructCollectionType(List.class, QuestionVO.OptionVO.class));
                    log.debug("[parseQuestionOptions] 成功解析为List<OptionVO>, 共{}项", vos.size());
                    return vos;
                } catch (Exception e2) {
                    log.debug("[parseQuestionOptions] List<OptionVO>解析失败: {}", e2.getMessage());
                }

                // 2.3 尝试兼容 Java toString 格式: [{key=A, value=选项1}]
                try {
                    String fixed = fixJavaToStringJson(optStr);
                    if (!fixed.equals(optStr)) {
                        List<Map<String, Object>> maps = mapper.readValue(fixed, new TypeReference<List<Map<String, Object>>>() {});
                        log.debug("[parseQuestionOptions] 兼容模式解析成功, 共{}项", maps.size());
                        return convertMapsToOptionVOs(maps);
                    }
                } catch (Exception e3) {
                    log.debug("[parseQuestionOptions] 兼容模式解析失败: {}", e3.getMessage());
                }

                log.warn("[parseQuestionOptions] 所有解析方式均失败，原始数据: {}", optStr);
                return null;
            }

            // 3. List 类型：List<Map> 或 List<String>
            if (options instanceof List) {
                List<?> optList = (List<?>) options;
                List<QuestionVO.OptionVO> result = new ArrayList<>();
                int idx = 0;
                for (Object item : optList) {
                    if (item == null) continue;
                    if (item instanceof Map) {
                        result.add(mapToOptionVO((Map<String, Object>) item));
                    } else if (item instanceof String) {
                        QuestionVO.OptionVO vo = new QuestionVO.OptionVO();
                        vo.setKey(String.valueOf((char) ('A' + idx)));
                        vo.setValue((String) item);
                        result.add(vo);
                        idx++;
                    }
                }
                return result.isEmpty() ? null : result;
            }
        } catch (Exception e) {
            log.error("[parseQuestionOptions] 解析题目选项失败，原始数据: {}", options, e);
        }
        return null;
    }

    /**
     * 将 Java toString 格式的伪 JSON 转换为标准 JSON
     * 例如: [{key=A, value=选项1}, {key=B, value=选项2}]
     * 转换为: [{"key":"A","value":"选项1"},{"key":"B","value":"选项2"}]
     */
    private String fixJavaToStringJson(String input) {
        if (!input.startsWith("[") || !input.endsWith("]")) return input;
        // 简单替换：给未加引号的key和value加上引号
        StringBuilder sb = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"' || c == '\'') {
                inString = !inString;
                sb.append(c);
            } else if (!inString && c == '=') {
                sb.append(":");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 将 Map 列表转换为 OptionVO 列表
     */
    private List<QuestionVO.OptionVO> convertMapsToOptionVOs(List<Map<String, Object>> maps) {
        List<QuestionVO.OptionVO> result = new ArrayList<>();
        for (Map<String, Object> map : maps) {
            result.add(mapToOptionVO(map));
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * 从 Map 中提取字段转换为 OptionVO（兼容多种字段名）
     */
    private QuestionVO.OptionVO mapToOptionVO(Map<String, Object> map) {
        QuestionVO.OptionVO vo = new QuestionVO.OptionVO();
        // key 字段：兼容 key / optionKey / label / optionLabel / id / optionId / name
        Object keyObj = getFirstMatch(map, "key", "optionKey", "label", "optionLabel", "id", "optionId", "name");
        vo.setKey(keyObj != null ? String.valueOf(keyObj) : "");
        // value 字段：兼容 value / optionContent / content / text / optionText / title / optionValue / description / name
        Object valObj = getFirstMatch(map, "value", "optionContent", "content", "text", "optionText", "title", "optionValue", "description", "name");
        vo.setValue(valObj != null ? String.valueOf(valObj) : "");
        return vo;
    }

    /**
     * 从 Map 中按优先级查找第一个匹配的值
     */
    private Object getFirstMatch(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                Object val = map.get(key);
                if (val != null) return val;
            }
        }
        return null;
    }

    /**
     * 解析知识点标签
     */
    private List<String> parseKnowledgeTags(Object tags) {
        if (tags == null) return null;
        if (tags instanceof String) {
            String tagStr = (String) tags;
            if (tagStr.trim().isEmpty()) return null;
            return Arrays.asList(tagStr.split(","));
        }
        if (tags instanceof List) {
            List<?> tagList = (List<?>) tags;
            List<String> result = new ArrayList<>();
            for (Object item : tagList) {
                result.add(String.valueOf(item));
            }
            return result;
        }
        return null;
    }
}
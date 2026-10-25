package com.smartedu.modules.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class KimiService {

    @Value("${kimi.api-key}")
    private String apiKey;

    @Value("${kimi.base-url}")
    private String baseUrl;

    @Value("${kimi.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 通用对话接口
     */
    public String chat(String systemPrompt, String userMessage) {
        // ===== 调试用：确认配置加载和方法调用 =====
        System.out.println("======= KimiService.chat() 被调用 =======");
        System.out.println("======= apiKey 是否为空: " + (apiKey == null || apiKey.isEmpty()) + " =======");
        System.out.println("======= apiKey 长度: " + (apiKey != null ? apiKey.length() : 0) + " =======");
        System.out.println("======= baseUrl: " + baseUrl + " =======");
        System.out.println("======= model: " + model + " =======");
        // ===== 调试结束 =====
        try {
            String url = baseUrl + "/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // 构建消息列表
            List<Map<String, String>> messages = new ArrayList<>();

            // 系统提示词
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                Map<String, String> systemMsg = new HashMap<>();
                systemMsg.put("role", "system");
                systemMsg.put("content", systemPrompt);
                messages.add(systemMsg);
            }

            // 用户消息
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            System.out.println("======= 正在发送请求到: " + url + " =======");
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            System.out.println("======= 响应状态: " + response.getStatusCode() + " =======");

            // 解析响应
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();
            System.out.println("======= AI回复: " + content.substring(0, Math.min(50, content.length())) + "... =======");
            return content;

        } catch (Exception e) {
            System.err.println("======= Kimi AI调用失败: " + e.getMessage() + " =======");
            e.printStackTrace();
            log.error("Kimi AI调用失败", e);
            return "AI服务暂时不可用，请稍后再试";
        }
    }

    /**
     * 智能问答 - 学生提问
     */
    public String answerQuestion(String question, String courseContext) {
        System.out.println("======= KimiService.answerQuestion() 被调用: question=" + question + " =======");
        String systemPrompt = "你是一位专业的教育AI助手，擅长解答学生的学习问题。" +
                "请用通俗易懂的语言回答，必要时可以举例说明。" +
                "如果问题与当前课程相关，请结合课程内容进行解答。\n\n" +
                "当前课程相关内容：\n" + courseContext;

        return chat(systemPrompt, question);
    }

    /**
     * 智能生成题目 - 教师使用（原始3参数版本，返回JSON字符串，向后兼容）
     */
    public String generateQuestions(String prompt, String questionType, int count) {
        String systemPrompt = "你是一位专业的教育出题专家，擅长根据要求生成高质量的试题。" +
                "请严格按照以下格式输出JSON格式的题目：\n" +
                "[\n" +
                "  {\n" +
                "    \"question\": \"题目内容\",\n" +
                "    \"options\": [\"选项A\", \"选项B\", \"选项C\", \"选项D\"],\n" +
                "    \"answer\": \"正确答案\",\n" +
                "    \"analysis\": \"答案解析\",\n" +
                "    \"difficulty\": 1-5,\n" +
                "    \"knowledgePoint\": \"知识点\"\n" +
                "  }\n" +
                "]\n" +
                "要求生成" + count + "道" + questionType + "题目。";

        return chat(systemPrompt, prompt);
    }

    /**
     * 智能生成题目 - 教师使用（5参数版本，返回结构化题目列表）
     */
    public List<Map<String, Object>> generateQuestions(Long courseId, String questionType,
                                                       int count, String difficulty, String prompt) {
        try {
            // 构建系统提示词
            String systemPrompt = buildQuestionGenPrompt(questionType, count, difficulty);

            // 构建用户提示词
            String userPrompt = "课程ID: " + courseId + "\n" +
                    "题型: " + questionType + "\n" +
                    "数量: " + count + "\n" +
                    "难度: " + difficulty + "\n\n" +
                    "出题要求:\n" + prompt;

            // 调用Kimi API
            String jsonResponse = chat(systemPrompt, userPrompt);
            log.info("[Kimi] 原始响应: {}", jsonResponse);

            // 解析并规范化
            return parseQuestions(jsonResponse, questionType, difficulty);
        } catch (Exception e) {
            log.error("[Kimi] 生成题目失败", e);
            throw new RuntimeException("AI生成题目失败: " + e.getMessage());
        }
    }

    /**
     * 学习推荐 - 根据学习情况推荐
     */
    public String recommendStudy(String studentProfile, String weakPoints) {
        String systemPrompt = "你是一位专业的学习规划师，擅长根据学生的学习情况制定个性化学习计划。" +
                "请分析学生的薄弱环节，给出具体的学习建议和推荐资源。";

        String userMessage = "学生情况：\n" + studentProfile + "\n\n薄弱环节：\n" + weakPoints;

        return chat(systemPrompt, userMessage);
    }

    // ==================== 私有方法 ====================

    private String buildQuestionGenPrompt(String questionType, int count, String difficulty) {
        return "你是一位专业的教育出题专家。请根据用户要求生成 " + count + " 道【" + difficulty +
                "】难度的【" + questionType + "】。\n" +
                "\n" +
                "【重要】必须严格遵守以下JSON格式返回，不要输出任何其他内容（如markdown标记、解释文字等）：\n" +
                "\n" +
                "[\n" +
                "  {\n" +
                "    \"questionContent\": \"题目内容（纯文本，不要带选项）\",\n" +
                "    \"questionOptions\": [\n" +
                "      {\"key\": \"A\", \"value\": \"选项A内容\"},\n" +
                "      {\"key\": \"B\", \"value\": \"选项B内容\"},\n" +
                "      {\"key\": \"C\", \"value\": \"选项C内容\"},\n" +
                "      {\"key\": \"D\", \"value\": \"选项D内容\"}\n" +
                "    ],\n" +
                "    \"correctAnswer\": \"A\",\n" +
                "    \"answerAnalysis\": \"答案解析\",\n" +
                "    \"difficultyLevel\": " + mapDifficulty(difficulty) + ",\n" +
                "    \"score\": 10\n" +
                "  }\n" +
                "]\n" +
                "\n" +
                "规则说明：\n" +
                "1. questionContent 只包含题干，不要包含选项字母\n" +
                "2. questionOptions 是选项数组，每个选项必须有 key（A/B/C/D）和 value（选项内容）\n" +
                "3. correctAnswer 是正确答案的 key，如 A、B、C 或 D\n" +
                "4. 对于判断题，questionOptions 固定为 [{\"key\":\"A\",\"value\":\"正确\"},{\"key\":\"B\",\"value\":\"错误\"}]\n" +
                "5. 对于填空题，questionOptions 可以为空数组 []，correctAnswer 直接填写答案文本\n" +
                "6. difficultyLevel: 简单=1, 中等=2, 困难=3\n" +
                "7. score 固定为 10\n" +
                "8. 必须返回合法的JSON数组，不要加 markdown 代码块标记";
    }

    private int mapDifficulty(String difficulty) {
        switch (difficulty) {
            case "简单": return 1;
            case "困难": return 3;
            default: return 2; // 中等
        }
    }

    /**
     * 从AI响应中解析题目列表
     */
    private List<Map<String, Object>> parseQuestions(String response, String questionType, String difficulty) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (response == null || response.trim().isEmpty()) {
            return result;
        }

        // 提取JSON数组
        String jsonStr = extractJsonArray(response);
        if (jsonStr == null) {
            log.warn("[Kimi] 无法从响应中提取JSON数组");
            return result;
        }

        try {
            List<Map<String, Object>> questions = objectMapper.readValue(jsonStr,
                    new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> q : questions) {
                Map<String, Object> normalized = normalizeQuestion(q, questionType, difficulty);
                if (normalized != null) {
                    result.add(normalized);
                }
            }
        } catch (Exception e) {
            log.error("[Kimi] JSON数组解析失败: {}", e.getMessage());
            // 尝试单题目解析
            try {
                Map<String, Object> single = objectMapper.readValue(jsonStr,
                        new TypeReference<Map<String, Object>>() {});
                Map<String, Object> normalized = normalizeQuestion(single, questionType, difficulty);
                if (normalized != null) {
                    result.add(normalized);
                }
            } catch (Exception e2) {
                log.error("[Kimi] 单题目解析也失败: {}", e2.getMessage());
            }
        }

        return result;
    }

    /**
     * 统一题目字段格式
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeQuestion(Map<String, Object> q, String questionType, String difficulty) {
        if (q == null) return null;

        Map<String, Object> normalized = new HashMap<>();

        // questionContent（兼容 question/questionContent/content 等）
        String content = extractString(q, "questionContent", "question", "content", "title", "stem");
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        normalized.put("questionContent", content);

        // questionOptions（兼容 options/questionOptions/choices 等）
        Object options = extractValue(q, "questionOptions", "options", "choices", "optionList");
        normalized.put("questionOptions", normalizeOptions(options, questionType));

        // correctAnswer（兼容 answer/correctAnswer/answerKey 等）
        String answer = extractString(q, "correctAnswer", "answer", "answerKey", "rightAnswer");
        normalized.put("correctAnswer", answer != null ? answer : "");

        // answerAnalysis（兼容 analysis/answerAnalysis/explanation 等）
        String analysis = extractString(q, "answerAnalysis", "analysis", "explanation", "parse");
        normalized.put("answerAnalysis", analysis != null ? analysis : "");

        // difficultyLevel
        Object diffLevel = extractValue(q, "difficultyLevel", "difficulty", "level");
        normalized.put("difficultyLevel", normalizeDifficultyLevel(diffLevel, difficulty));

        // score
        Object score = q.get("score");
        normalized.put("score", score != null ? score : 10);

        return normalized;
    }

    /**
     * 统一选项格式为 [{key: "A", value: "xxx"}, ...]
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeOptions(Object options, String questionType) {
        // 判断题固定选项
        if ("判断题".equals(questionType)) {
            List<Map<String, Object>> tfOptions = new ArrayList<>();
            tfOptions.add(createOption("A", "正确"));
            tfOptions.add(createOption("B", "错误"));
            return tfOptions;
        }
        // 填空题无选项
        if ("填空题".equals(questionType)) {
            return new ArrayList<>();
        }
        if (options == null) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        if (options instanceof List) {
            List<Object> optList = (List<Object>) options;
            for (int i = 0; i < optList.size(); i++) {
                Object opt = optList.get(i);
                if (opt instanceof Map) {
                    Map<String, Object> optMap = (Map<String, Object>) opt;
                    String key = extractString(optMap, "key", "label", "option");
                    String value = extractString(optMap, "value", "text", "content", "desc");
                    if (key == null) {
                        key = String.valueOf((char) ('A' + i));
                    }
                    if (value != null) {
                        result.add(createOption(key.toUpperCase(), value));
                    }
                } else if (opt instanceof String) {
                    String key = String.valueOf((char) ('A' + i));
                    result.add(createOption(key, (String) opt));
                }
            }
        } else if (options instanceof String) {
            try {
                List<Object> parsed = objectMapper.readValue((String) options, List.class);
                return normalizeOptions(parsed, questionType);
            } catch (Exception e) {
                log.warn("[Kimi] 选项字符串解析失败: {}", options);
            }
        }
        return result;
    }

    private Map<String, Object> createOption(String key, String value) {
        Map<String, Object> opt = new HashMap<>();
        opt.put("key", key);
        opt.put("value", value);
        return opt;
    }

    private int normalizeDifficultyLevel(Object level, String defaultDifficulty) {
        if (level instanceof Number) {
            return ((Number) level).intValue();
        }
        if (level instanceof String) {
            try {
                return Integer.parseInt((String) level);
            } catch (NumberFormatException e) {
                String str = ((String) level).trim();
                if (str.contains("简单")) return 1;
                if (str.contains("困难") || str.contains("难")) return 3;
                return 2;
            }
        }
        switch (defaultDifficulty) {
            case "简单": return 1;
            case "困难": return 3;
            default: return 2;
        }
    }

    /**
     * 从响应中提取JSON数组
     */
    private String extractJsonArray(String response) {
        if (response == null) return null;
        String cleaned = response.trim();
        // 去除markdown代码块
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "").trim();
        }
        int start = cleaned.indexOf('[');
        int end = cleaned.lastIndexOf(']');
        if (start != -1 && end != -1 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        // 没有方括号，尝试花括号（单个对象）
        start = cleaned.indexOf('{');
        end = cleaned.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    private String extractString(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val instanceof String && !((String) val).trim().isEmpty()) {
                return (String) val;
            }
        }
        return null;
    }

    private Object extractValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }
}
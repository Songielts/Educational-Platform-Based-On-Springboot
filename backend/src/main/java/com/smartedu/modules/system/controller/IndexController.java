package com.smartedu.modules.system.controller;

import com.smartedu.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 首页控制器
 */
@RestController
public class IndexController {

    /**
     * 首页信息
     */
    @GetMapping("/")
    public Result<Map<String, Object>> index() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "智慧教育平台");
        info.put("version", "1.0.0");
        info.put("description", "基于Spring Boot的在线学习平台");
        info.put("apiBase", "/api");
        info.put("docs", "/api/swagger-ui.html");
        return Result.success(info);
    }
}

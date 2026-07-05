package com.virtualwife.admin.module.llm.controller;

import com.virtualwife.admin.common.result.Result;
import com.virtualwife.admin.module.llm.service.LlmAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * LLM 分析接口 - 供 RAG 服务调用
 */
@Slf4j
@RestController
@RequestMapping("/llm/analysis")
@RequiredArgsConstructor
public class LlmAnalysisController {

    private final LlmAnalysisService llmAnalysisService;

    /**
     * 使用 LLM 分析文本内容
     * POST /api/admin/llm/analysis/text
     */
    @PostMapping("/text")
    public Result<Map<String, Object>> analyzeText(@RequestBody Map<String, String> body) {
        String systemPrompt = body.getOrDefault("systemPrompt", "你是一个专业的数据分析师。");
        String userPrompt = body.get("userPrompt");

        if (userPrompt == null || userPrompt.isBlank()) {
            return Result.badRequest("userPrompt 不能为空");
        }

        try {
            String analysis = llmAnalysisService.analyze(systemPrompt, userPrompt);
            return Result.success(Map.of("analysis", analysis));
        } catch (Exception e) {
            log.error("LLM 分析失败", e);
            return Result.error("LLM 分析失败: " + e.getMessage());
        }
    }
}
